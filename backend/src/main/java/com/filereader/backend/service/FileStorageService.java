package com.filereader.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${filereader.storage.upload-dir}") String uploadDirPath) {
        this.uploadDir = Path.of(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel criar o diretorio de uploads: " + uploadDir, e);
        }
    }

    public ArquivoArmazenado armazenar(MultipartFile arquivo) {
        try {
            byte[] bytes = arquivo.getBytes();
            String hash = calcularHashSha256(bytes);
            String nomeOriginal = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "arquivo";
            String nomeUnico = UUID.randomUUID() + "_" + sanitizar(nomeOriginal);
            Path destino = uploadDir.resolve(nomeUnico);
            Files.write(destino, bytes);
            return new ArquivoArmazenado(nomeOriginal, destino.toString(), hash, bytes.length, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao armazenar arquivo", e);
        }
    }

    private String sanitizar(String nomeArquivo) {
        return nomeArquivo.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String calcularHashSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel", e);
        }
    }
}
