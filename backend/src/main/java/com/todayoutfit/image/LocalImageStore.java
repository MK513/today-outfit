package com.todayoutfit.image;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

/**
 * 로컬 디스크 저장소. 키 "{userId}/{file}"을 {dir}/{userId}/{file}에 저장하고,
 * {publicPath}/{key} URL로 {@link LocalImageController}가 파일을 내려준다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.image.storage", havingValue = "local", matchIfMissing = true)
public class LocalImageStore implements ImageStore {

    private final Path root;
    private final String publicPath;

    public LocalImageStore(ImageProperties properties) {
        this.root = properties.local().dir().toAbsolutePath().normalize();
        this.publicPath = properties.local().publicPath();
    }

    @Override
    public void put(String key, InputStream content, long size, MediaType type) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store image " + key, e);
        }
    }

    @Override
    public Optional<Resource> get(String key) {
        Path path = resolve(key);
        return Files.isRegularFile(path) ? Optional.of(new FileSystemResource(path)) : Optional.empty();
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Failed to delete image {}", key, e);
        }
    }

    @Override
    public void deleteAll(String prefix) {
        try {
            FileSystemUtils.deleteRecursively(resolve(prefix));
        } catch (IOException e) {
            log.warn("Failed to delete images under {}", prefix, e);
        }
    }

    @Override
    public List<StoredImage> list() {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> new StoredImage(toKey(file), lastModified(file)))
                    .toList();
        } catch (IOException e) {
            log.warn("Failed to list images", e);
            return List.of();
        }
    }

    @Override
    public String publicUrl(String key) {
        return publicPath + "/" + key;
    }

    @Override
    public Optional<String> keyOf(String publicUrl) {
        String prefix = publicPath + "/";
        if (publicUrl == null || !publicUrl.startsWith(prefix)) {
            return Optional.empty();
        }
        return Optional.of(publicUrl.substring(prefix.length()));
    }

    /** 키를 저장 폴더 안의 경로로 바꾼다. 폴더 밖을 가리키면 거부한다. */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Image key escapes storage root: " + key);
        }
        return path;
    }

    private String toKey(Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static java.time.Instant lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            return java.time.Instant.now();
        }
    }
}
