package com.tanle.tland.upload_service.service;

import com.tanle.tland.upload_service.grpc.FileChunk;
import com.tanle.tland.upload_service.grpc.UploadResponse;
import com.tanle.tland.upload_service.grpc.UploadServiceGrpc;
import com.tanle.tland.upload_service.utils.Helper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class UploadServiceGrpcImpl extends UploadServiceGrpc.UploadServiceImplBase {
    private final S3Client s3Client;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Override
    public StreamObserver<FileChunk> upload(StreamObserver<UploadResponse> responseObserver) {
        return new StreamObserver<FileChunk>() {
            private ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            private String fileName;

            @Override
            public void onNext(FileChunk fileChunk) {
                try {
                    if (fileName == null) {
                        fileName = fileChunk.getFileName();
                    }
                    byteStream.write(fileChunk.getContent().toByteArray());
                } catch (IOException e) {
                    log.error("Error buffering file chunk", e);
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error during upload", throwable);
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                try {
                    byte[] fileBytes = byteStream.toByteArray();
                    String contentType = Files.probeContentType(Paths.get(fileName));
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }

                    PutObjectRequest putRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .contentType(contentType)
                            .build();

                    s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
                    log.info("File uploaded to S3: {}", fileName);

                    String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);

                    String type = "other";
                    double duration = 0;

                    if (contentType.startsWith("image/")) {
                        type = "image";
                    } else if (contentType.startsWith("video/")) {
                        type = "video";

                        try {
                            Path tempFile = Files.createTempFile("temp-", fileName);
                            Files.write(tempFile, fileBytes);
                            duration = Helper.getVideoDuration(tempFile);
                            Files.deleteIfExists(tempFile);
                        } catch (Exception e) {
                            log.warn("Could not extract video duration", e);
                        }
                    }

                    UploadResponse.Builder responseBuilder = UploadResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("File uploaded: " + fileName)
                            .setUrl(url)
                            .setType(type);

                    if ("video".equals(type)) {
                        responseBuilder.setDuration(duration);
                    }


                    responseObserver.onNext(responseBuilder.build());
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    log.error("Upload failed", e);
                    responseObserver.onError(e);
                }
            }
        };
    }


}
