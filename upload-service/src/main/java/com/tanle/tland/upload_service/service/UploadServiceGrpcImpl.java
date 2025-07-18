package com.tanle.tland.upload_service.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.tanle.tland.upload_service.grpc.FileChunk;
import com.tanle.tland.upload_service.grpc.UploadResponse;
import com.tanle.tland.upload_service.grpc.UploadServiceGrpc;
import com.tanle.tland.upload_service.utils.Helper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class UploadServiceGrpcImpl extends UploadServiceGrpc.UploadServiceImplBase {
    private final AmazonS3 s3Client;

    @Value("${aws.s3.bucket_name}")
    private String bucketName;

    @Override
    public StreamObserver<FileChunk> upload(StreamObserver<UploadResponse> responseObserver) {
        StreamObserver<FileChunk> observer = new StreamObserver<FileChunk>() {
            private OutputStream outputStream;
            private String fileName;
            private Path localPath;

            @Override
            public void onNext(FileChunk fileChunk) {
                try {
                    if (outputStream == null) {
                        this.fileName = fileChunk.getFileName();
                        localPath = Paths.get("uploads", fileName);
                        Files.createDirectories(localPath.getParent());
                        outputStream = Files.newOutputStream(localPath);
                    }
                    outputStream.write(fileChunk.getContent().toByteArray());
                } catch (IOException e) {
                    log.error("Error while writing file", e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                throw new RuntimeException(throwable);
            }

            @Override
            public void onCompleted() {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }

                    String contentType = Files.probeContentType(localPath);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }

                    try (InputStream inputStream = Files.newInputStream(localPath)) {
                        ObjectMetadata metadata = new ObjectMetadata();
                        metadata.setContentLength(Files.size(localPath));
                        metadata.setContentType(contentType);

                        PutObjectRequest request = new PutObjectRequest(
                                bucketName,
                                fileName,
                                inputStream,
                                metadata
                        );

                        s3Client.putObject(request);
                        log.info("File uploaded to S3: {}", fileName);
                    }

                    // Construct S3 URL
                    String url = "https://" + bucketName + ".s3." + s3Client.getRegionName()
                            + ".amazonaws.com/" + fileName;

                    String type = "other";
                    double duration = 0;

                    if (contentType.startsWith("image/")) {
                        type = "image";
                    } else if (contentType.startsWith("video/")) {
                        type = "video";
                        try {
                            duration = Helper.getVideoDuration(localPath); // Implement this
                        } catch (Exception e) {
                            log.warn("Failed to get video duration", e);
                        }
                    }

                    // Build response
                    UploadResponse.Builder responseBuilder = UploadResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("File uploaded: " + fileName)
                            .setUrl(url)
                            .setType(type);

                    if (type.equals("video")) {
                        responseBuilder.setDuration(duration);
                    }

                    responseObserver.onNext(responseBuilder.build());
                    responseObserver.onCompleted();
                } catch (IOException e) {
                    log.error("Upload error", e);
                    responseObserver.onError(e);
                }
            }
        };

        return observer;
    }
}
