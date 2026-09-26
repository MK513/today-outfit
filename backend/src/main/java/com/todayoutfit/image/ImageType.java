package com.todayoutfit.image;

import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.MediaType;

/** 허용하는 사진 형식. 확장자나 Content-Type이 아니라 파일 앞부분의 시그니처로 판별한다. */
enum ImageType {
    JPEG("jpg", MediaType.IMAGE_JPEG, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("png", MediaType.IMAGE_PNG, new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});

    static final int SIGNATURE_LENGTH = 8;

    final String extension;
    final MediaType mediaType;
    private final byte[] signature;

    ImageType(String extension, MediaType mediaType, byte[] signature) {
        this.extension = extension;
        this.mediaType = mediaType;
        this.signature = signature;
    }

    static Optional<ImageType> detect(byte[] head) {
        return Arrays.stream(values()).filter(t -> t.matches(head)).findFirst();
    }

    static Optional<ImageType> fromExtension(String extension) {
        return Arrays.stream(values()).filter(t -> t.extension.equals(extension)).findFirst();
    }

    private boolean matches(byte[] head) {
        if (head.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (head[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
