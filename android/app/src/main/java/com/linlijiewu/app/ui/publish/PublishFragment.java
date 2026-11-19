package com.linlijiewu.app.ui.publish;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.remote.ApiResponse;
import com.linlijiewu.app.data.remote.ImageUploadData;
import com.linlijiewu.app.data.remote.ItemDetail;
import com.linlijiewu.app.data.remote.PublishItemRequest;
import com.linlijiewu.app.data.repository.PublishRepository;
import com.linlijiewu.app.data.remote.ApiClient;
import com.linlijiewu.app.databinding.FragmentPublishBinding;
import com.linlijiewu.app.ui.detail.ItemDetailActivity;
import com.linlijiewu.app.util.ImageCompressor;
import com.linlijiewu.app.util.ItemCategory;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 发布页（第 2 周）：名称 / 分类 / 描述表单 + 相册图片选择与压缩（{@link ImageCompressor}）。
 * 提交流程：压缩图 → POST /images 上传 → POST /items 创建；失败保留全部输入。
 * 公共交接点由方舒煜的选点组件提供，接入前使用 local.properties 的 debug.mockLocation
 * 作为调试交接点；两者皆无时不允许提交。
 */
public class PublishFragment extends Fragment {

    private static final int TITLE_MAX = 50;
    private static final int DESCRIPTION_MAX = 500;

    private FragmentPublishBinding binding;
    private PublishRepository repository;
    private Call<?> activeCall;
    private byte[] compressedJpeg;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    compressImage(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPublishBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new PublishRepository(ApiClient.create(BuildConfig.SERVER_BASE_URL, () -> null));

        binding.inputCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, categoryLabels()));
        binding.inputCategory.setText(getString(ItemCategory.TOOL.labelRes), false);

        binding.pickImageButton.setOnClickListener(v -> pickImageLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        binding.repickButton.setOnClickListener(v -> pickImageLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        binding.submitButton.setOnClickListener(v -> submit());
    }

    private String[] categoryLabels() {
        ItemCategory[] categories = ItemCategory.values();
        String[] labels = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            labels[i] = getString(categories[i].labelRes);
        }
        return labels;
    }

    private ItemCategory selectedCategory() {
        String label = binding.inputCategory.getText().toString();
        for (ItemCategory category : ItemCategory.values()) {
            if (getString(category.labelRes).equals(label)) {
                return category;
            }
        }
        return ItemCategory.OTHER;
    }

    private void compressImage(Uri uri) {
        binding.imageInfo.setText(R.string.publish_image_compressing);
        binding.pickImageButton.setEnabled(false);
        ioExecutor.execute(() -> {
            try {
                ImageCompressor.Result result = ImageCompressor.compress(requireContext(), uri);
                mainHandler.post(() -> onImageCompressed(result));
            } catch (IOException e) {
                mainHandler.post(() -> {
                    binding.pickImageButton.setEnabled(true);
                    binding.imageInfo.setText(R.string.publish_image_none);
                    showError(getString(R.string.publish_image_compress_failed));
                });
            }
        });
    }

    private void onImageCompressed(ImageCompressor.Result result) {
        compressedJpeg = result.jpeg;
        Bitmap preview = BitmapFactory.decodeByteArray(result.jpeg, 0, result.jpeg.length);
        binding.imagePreview.setImageBitmap(preview);
        binding.imageInfo.setText(getString(R.string.publish_image_info,
                result.width, result.height, Formatter.formatShortFileSize(requireContext(), result.jpeg.length)));
        binding.pickImageButton.setVisibility(View.GONE);
        binding.repickButton.setVisibility(View.VISIBLE);
        binding.pickImageButton.setEnabled(true);
    }

    @Nullable
    private String validate() {
        String title = binding.inputTitle.getText() == null
                ? "" : binding.inputTitle.getText().toString().trim();
        if (title.isEmpty()) {
            return getString(R.string.publish_error_title_empty);
        }
        if (title.length() > TITLE_MAX) {
            return getString(R.string.publish_error_title_long);
        }
        String description = binding.inputDescription.getText() == null
                ? "" : binding.inputDescription.getText().toString().trim();
        if (description.length() > DESCRIPTION_MAX) {
            return getString(R.string.publish_error_description_long);
        }
        if (resolvePickup() == null) {
            return getString(R.string.publish_error_pickup_required);
        }
        return null;
    }

    /**
     * 调试交接点：仅当 local.properties 配置了 debug.mockLocation 时可用；
     * 方舒煜的选点组件接入后改为由选点结果提供。
     */
    @Nullable
    private double[] resolvePickup() {
        String mock = BuildConfig.MOCK_LOCATION;
        if (mock == null || mock.isEmpty()) {
            return null;
        }
        String[] parts = mock.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            return new double[]{lat, lng};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void submit() {
        if (activeCall != null) {
            return;
        }
        String error = validate();
        if (error != null) {
            showError(error);
            return;
        }
        setSubmitting(true, compressedJpeg != null
                ? R.string.publish_uploading : R.string.publish_submitting);
        if (compressedJpeg != null) {
            uploadImageThenPublish();
        } else {
            publishItem(null);
        }
    }

    private void uploadImageThenPublish() {
        Call<ApiResponse<ImageUploadData>> call = repository.uploadImage(compressedJpeg);
        activeCall = call;
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ImageUploadData>> call,
                                   @NonNull Response<ApiResponse<ImageUploadData>> response) {
                ApiResponse<ImageUploadData> body = response.body();
                if (response.isSuccessful() && body != null && body.getCode() == 0
                        && body.getData() != null) {
                    publishItem(body.getData().getImageId());
                } else {
                    onRequestFailed(response.code(), body == null ? null : body.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ImageUploadData>> call,
                                  @NonNull Throwable throwable) {
                onNetworkFailed();
            }
        });
    }

    private void publishItem(@Nullable String imageId) {
        String description = binding.inputDescription.getText() == null
                ? "" : binding.inputDescription.getText().toString().trim();
        double[] pickup = resolvePickup();
        PublishItemRequest request = new PublishItemRequest(
                binding.inputTitle.getText().toString().trim(),
                selectedCategory().apiValue,
                description.isEmpty() ? null : description,
                imageId,
                pickup == null ? null : pickup[0],
                pickup == null ? null : pickup[1],
                getString(R.string.publish_pickup_debug_label));
        Call<ApiResponse<ItemDetail>> call = repository.publishItem(request);
        activeCall = call;
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ItemDetail>> call,
                                   @NonNull Response<ApiResponse<ItemDetail>> response) {
                activeCall = null;
                setSubmitting(false, 0);
                ApiResponse<ItemDetail> body = response.body();
                if (response.isSuccessful() && body != null && body.getCode() == 0
                        && body.getData() != null) {
                    onPublished(body.getData());
                } else {
                    onRequestFailed(response.code(), body == null ? null : body.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ItemDetail>> call,
                                  @NonNull Throwable throwable) {
                activeCall = null;
                setSubmitting(false, 0);
                onNetworkFailed();
            }
        });
    }

    private void onPublished(ItemDetail detail) {
        resetForm();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.publish_success)
                .setMessage(getString(R.string.publish_success_message, detail.getTitle()))
                .setPositiveButton(R.string.publish_view_detail, (dialog, which) ->
                        ItemDetailActivity.start(requireContext(), detail))
                .setNegativeButton(R.string.publish_continue, null)
                .show();
    }

    private void onRequestFailed(int httpCode, @Nullable String message) {
        if (httpCode == 401) {
            showError(getString(R.string.publish_error_unauthorized));
        } else if (message != null && !message.isEmpty()) {
            showError(getString(R.string.publish_error_generic, message));
        } else {
            showError(getString(R.string.publish_error_generic,
                    getString(R.string.publish_error_unknown)));
        }
    }

    private void onNetworkFailed() {
        showError(getString(R.string.publish_error_network));
    }

    private void setSubmitting(boolean submitting, int statusRes) {
        binding.submitButton.setEnabled(!submitting);
        binding.pickImageButton.setEnabled(!submitting);
        binding.repickButton.setEnabled(!submitting);
        binding.inputTitle.setEnabled(!submitting);
        binding.inputCategory.setEnabled(!submitting);
        binding.inputDescription.setEnabled(!submitting);
        if (submitting) {
            binding.publishProgress.setVisibility(View.VISIBLE);
            binding.submitButton.setText(statusRes);
        } else {
            binding.publishProgress.setVisibility(View.GONE);
            binding.submitButton.setText(R.string.publish_submit);
        }
    }

    private void resetForm() {
        binding.inputTitle.setText("");
        binding.inputDescription.setText("");
        binding.imagePreview.setImageDrawable(null);
        binding.imageInfo.setText(R.string.publish_image_none);
        binding.pickImageButton.setVisibility(View.VISIBLE);
        binding.repickButton.setVisibility(View.GONE);
        compressedJpeg = null;
    }

    private void showError(String message) {
        View root = getView();
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
        ioExecutor.shutdown();
        binding = null;
        super.onDestroyView();
    }
}
