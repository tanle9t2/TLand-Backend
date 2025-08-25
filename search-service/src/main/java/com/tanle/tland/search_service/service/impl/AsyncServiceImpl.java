package com.tanle.tland.search_service.service.impl;

import com.tanle.tland.search_service.entity.PostDocument;
import com.tanle.tland.search_service.mapper.PostMapper;
import com.tanle.tland.search_service.service.AsyncService;
import com.tanle.tland.user_serivce.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import static com.tanle.tland.search_service.utils.AppConstant.INDEX_NAME;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncServiceImpl implements AsyncService {
    private final ElasticsearchOperations elasticsearchOperations;
    private final PostMapper postMapper;

    @GrpcClient("postServiceGrpc")
    private PostToSearchServiceGrpc.PostToSearchServiceBlockingStub postToSearchServiceBlockingStub;

    @Override
    public void createPost(String postId) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(INDEX_NAME);
        IndexOperations indexOperations = elasticsearchOperations.indexOps(indexCoordinates);
        if (!indexOperations.exists()) {
            indexOperations.create();
            System.out.println("Index created: " + INDEX_NAME);
        }

        PostDetailResponse responses = postToSearchServiceBlockingStub.getPostById(
                PostDetailRequest.newBuilder()
                        .setId(postId)
                        .build()
        );
        PostDocument postDocument = postMapper.convertToDocument(responses);

        log.info("Create post - {}", postDocument.getId());
        elasticsearchOperations.save(postDocument);
    }

    @Override
    public void updatePost(String postId) {
        PostDetailResponse responses = postToSearchServiceBlockingStub.getPostById(
                PostDetailRequest.newBuilder()
                        .setId(postId)
                        .build()
        );
        PostDocument postNewDocument = postMapper.convertToDocument(responses);
        elasticsearchOperations.save(postNewDocument);
    }

    @Override
    public void deletePost(String postId) {
        elasticsearchOperations.delete(String.valueOf(postId), IndexCoordinates.of(INDEX_NAME));
    }

    @Override
    public void migrateData() {
        // Check if the index exists
        IndexOperations indexOps = elasticsearchOperations.indexOps(PostDocument.class);
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping(PostDocument.class));
        }
        PostDetailResponseList responses = postToSearchServiceBlockingStub.getAllPost(Empty.newBuilder().build());

        for (var post : responses.getResponseList()) {
            PostDocument postDocument = postMapper.convertToDocument(post);
            elasticsearchOperations.save(postDocument);
            log.info("Create post - {}", post.getId());
        }

    }


}
