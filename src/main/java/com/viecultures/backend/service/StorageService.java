package com.viecultures.backend.service;

import com.viecultures.backend.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Tải tệp lên Cloudflare R2 và trả về thông tin tệp kèm đường dẫn công khai.
     *
     * @param file   tệp dữ liệu cần tải lên
     * @param folder thư mục phân loại tài nguyên (ví dụ: journeys, avatars, posts)
     * @return FileUploadResponse chứa đường dẫn công khai, dung lượng và định dạng tệp
     */
    FileUploadResponse uploadFile(MultipartFile file, String folder);
}
