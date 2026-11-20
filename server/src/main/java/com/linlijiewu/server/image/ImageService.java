package com.linlijiewu.server.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import com.linlijiewu.server.common.BusinessException;

@Service
public class ImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);
    private final ImageMapper images;
    private final Path root;
    public record Uploaded(String imageId, String url) { }

    public ImageService(ImageMapper images, Environment environment) {
        this.images = images;
        root = Path.of(environment.getProperty("linli.upload-dir", "./uploads")).toAbsolutePath().normalize();
    }

    @Transactional
    public Uploaded upload(long userId, MultipartFile file) {
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BusinessException(413, 41301, "图片不能超过 5 MB");
        }
        if (file.isEmpty()) { throw BusinessException.badRequest("请选择图片"); }
        byte[] png = reencode(file);
        ImageRow image = new ImageRow();
        image.imageId = UUID.randomUUID().toString().replace("-", "");
        image.ownerId = userId;
        image.fileName = image.imageId + ".png";
        image.sizeBytes = png.length;
        Path target = root.resolve(image.fileName);
        try {
            Files.createDirectories(root);
            Files.write(target, png, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new IllegalStateException("图片保存失败", exception);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { Files.deleteIfExists(target); }
                    catch (IOException exception) { LOGGER.warn("回滚图片文件清理失败", exception); }
                }
            }
        });
        images.insert(image);
        return new Uploaded(image.imageId, "/files/" + image.fileName);
    }

    public String resolveOwned(String id, long userId) {
        if (id == null) { return null; }
        if (!id.matches("[a-f0-9]{32}")) { throw BusinessException.badRequest("图片编号不正确"); }
        ImageRow image = images.find(id);
        if (image == null) { throw BusinessException.badRequest("图片不存在，请重新上传"); }
        if (image.ownerId != userId) { throw BusinessException.forbidden(); }
        if (!Files.isRegularFile(root.resolve(image.fileName))) {
            throw BusinessException.badRequest("图片文件不存在，请重新上传");
        }
        return "/files/" + image.fileName;
    }

    public Path file(String name) {
        if (!name.matches("[a-f0-9]{32}\\.png") || images.findByFileName(name) == null) {
            throw BusinessException.notFound();
        }
        Path file = root.resolve(name).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) { throw BusinessException.notFound(); }
        return file;
    }

    private byte[] reencode(MultipartFile file) {
        try (var input = file.getInputStream(); var stream = ImageIO.createImageInputStream(input)) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) { throw BusinessException.badRequest("仅支持真实的 JPG、PNG、WebP 图片"); }
            var reader = readers.next();
            try {
                if (!Set.of("jpeg", "jpg", "png", "webp").contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw BusinessException.badRequest("仅支持 JPG、PNG、WebP 图片");
                }
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 8192 || height > 8192
                        || (long) width * height > 20_000_000) {
                    throw BusinessException.badRequest("图片尺寸过大，请压缩后重试");
                }
                BufferedImage original = reader.read(0);
                BufferedImage clean = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = clean.createGraphics();
                try { graphics.drawImage(original, 0, 0, null); }
                finally { graphics.dispose(); original.flush(); }
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    if (!ImageIO.write(clean, "png", output)) { throw new IOException("PNG writer unavailable"); }
                    return output.toByteArray();
                } finally { clean.flush(); }
            } finally { reader.dispose(); }
        } catch (IOException exception) {
            throw BusinessException.badRequest("图片损坏或无法解码");
        }
    }
}
