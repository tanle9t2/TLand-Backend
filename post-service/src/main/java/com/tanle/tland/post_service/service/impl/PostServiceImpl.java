package com.tanle.tland.post_service.service.impl;


import com.tanle.tland.payment_service.grpc.PaymentServiceGrpc;
import com.tanle.tland.payment_service.grpc.PaymentUrlRequest;
import com.tanle.tland.payment_service.grpc.PaymentUrlResponse;
import com.tanle.tland.post_service.entity.*;
import com.tanle.tland.post_service.exception.ResourceNotFoundExeption;
import com.tanle.tland.post_service.exception.UnauthorizedException;
import com.tanle.tland.post_service.grpc.UserInfoRequest;
import com.tanle.tland.post_service.grpc.UserPostInfoResponse;
import com.tanle.tland.post_service.grpc.UserToPostServiceGrpc;
import com.tanle.tland.post_service.mapper.AssetMapper;
import com.tanle.tland.post_service.mapper.PostMapper;
import com.tanle.tland.post_service.mapper.UserMapper;
import com.tanle.tland.post_service.projection.PostHistory;
import com.tanle.tland.post_service.projection.PostOverview;
import com.tanle.tland.post_service.projection.StatusCount;
import com.tanle.tland.post_service.repo.CommentRepo;
import com.tanle.tland.post_service.repo.PostRepo;
import com.tanle.tland.post_service.request.PostCreateRequest;
import com.tanle.tland.post_service.response.*;
import com.tanle.tland.post_service.response.PostDetailResponse;
import com.tanle.tland.post_service.service.PostService;
import com.tanle.tland.post_service.utils.AppConstant;
import com.tanle.tland.post_service.utils.Helper;
import com.tanle.tland.user_serivce.grpc.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ForbiddenException;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    @GrpcClient("assetServiceGrpc")
    private AssetToPostServiceGrpc.AssetToPostServiceBlockingStub assetToPostServiceBlockingStub;
    @GrpcClient("userServiceGrpc")
    private UserToPostServiceGrpc.UserToPostServiceBlockingStub userToPostServiceBlockingStub;
    @GrpcClient("paymentServiceGrpc")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentServiceBlockingStub;
    private final PostRepo postRepo;
    private final CommentRepo commentRepo;
    private final PostMapper postMapper;
    private final AssetMapper assetMapper;
    private final UserMapper userMapper;


    @Override
    @Transactional
    public MessageResponse createPost(PostCreateRequest request, String userId, HttpServletRequest httpServletRequest) {
        assetToPostServiceBlockingStub.checkExisted(AssetRequest.newBuilder()
                .setId(request.getAssetId())
                .setUserId(userId)
                .build());

        Post post = postMapper.convertToEntity(request);
        post.setUserId(userId);
        post.setStatus(PostStatus.WAITING_PAYMENT);
        postRepo.save(post);
        postRepo.flush();

        PaymentUrlResponse paymentUrlResponse = paymentServiceBlockingStub.getPaymentUrl(
                PaymentUrlRequest.newBuilder()
                        .setAmount(AppConstant.PRICE_CREATED)
                        .setPurposeType(PurposeType.POST.name())
                        .setTransactionType(TransactionType.CREATED.name())
                        .setIpAddress(Helper.getIpAddress(httpServletRequest))
                        .setTxnRef(post.getId())
                        .build()
        );

        return MessageResponse.builder()
                .message("Successfully create post")
                .status(HttpStatus.CREATED)
                .data(Map.of("paymentUrl", paymentUrlResponse.getVnpUrl()))
                .build();
    }

    @Override
    @Transactional
    public MessageResponse updatePost(String postId, String userId, PostCreateRequest request) throws AccessDeniedException {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));
        if (!post.getUserId().equals(userId))
            throw new AccessDeniedException("Don't have permission for this resource");


        postMapper.updatePost(request, post);
        postRepo.save(post);
        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .data(Map.of("id", post.getId()))
                .message("Successfully update post")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse inActivePost(String userId, List<String> roles, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        if (!post.getUserId().equals(userId) && !roles.contains(UserRole.ROLE_ADMIN.name()))
            throw new UnauthorizedException("Don't have permission for this resource");

        post.setStatus(PostStatus.DELETE);
        return MessageResponse.builder()
                .message("Successfully inactive post")
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public MessageResponse hidePost(String userId, List<String> roles, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));
        if (post.getStatus() != PostStatus.SHOW)
            throw new RuntimeException("Invalid status");

        if (!post.getUserId().equals(userId) && !roles.contains(UserRole.ROLE_ADMIN.name()))
            throw new UnauthorizedException("Don't have permission for this resource");

        post.setStatus(PostStatus.HIDE);
        postRepo.save(post);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully hide post")
                .data(Map.of("id", postId))
                .build();

    }

    @Override
    public MessageResponse acceptPost(String userId, List<String> roles, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        if (!post.getUserId().equals(userId) && !roles.contains(UserRole.ROLE_ADMIN.name()))
            throw new UnauthorizedException("Don't have permission for this resource");

        post.setStatus(PostStatus.SHOW);
        postRepo.save(post);
        return MessageResponse.builder()
                .message("Successfully accept post")
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public void updateStatusPost(String postId, PostStatus status) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        post.setStatus(status);
        postRepo.save(post);
    }

    @Override
    @Transactional
    public void deletePost(String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        postRepo.delete(post);
    }

    @Override
    public MessageResponse rejectPost(List<String> roles, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        if (roles == null || !roles.contains(UserRole.ROLE_ADMIN.name()))
            throw new UnauthorizedException("Don't have permission for this resource");

        post.setStatus(PostStatus.REJECT);
        postRepo.save(post);
        return MessageResponse.builder()
                .message("Successfully accept post")
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse likePost(String userId, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        PostLike postLike = PostLike.builder()
                .postId(postId)
                .userId(userId)
                .build();

        post.addLikePost(postLike);

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully like post")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse unlikePost(String userId, String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        post.removeLikePost(userId);
        postRepo.save(post);
        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Successfully like post")
                .build();
    }

    @Override
    public PostDetailResponse findPostById(String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        PostDetailResponse postDetailResponse = postMapper.convertToResponse(post);

        return postDetailResponse;
    }

    @Override
    public PageResponse<PostHistoryResponse> findHistoryPost(String assetId, String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostHistory> histories = postRepo.findAllByAssetIdAndUserId(assetId, userId, pageable);

        List<PostHistoryResponse> data = histories.get()
                .map(p -> postMapper.convertToResponse(p))
                .collect(Collectors.toList());

        return PageResponse.<PostHistoryResponse>builder()
                .content(data)
                .last(histories.isLast())
                .size(histories.getSize())
                .totalElements(histories.getTotalElements())
                .totalPages(histories.getTotalPages())
                .build();
    }

    @Override
    public PageResponse<PostResponse> findAll(int page, int limit, String type) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepo.findAllByType(pageable, PostType.valueOf(type), PostStatus.SHOW);

        List<PostResponse> posts = postPage.stream().
                map(p -> {
                    AssetResponse assetResponse = assetToPostServiceBlockingStub.getAssetDetail(AssetRequest.newBuilder()
                            .setId(p.getAssetId())
                            .build());

                    PostResponse postResponse = postMapper.convertToPostDetailResponse(p);
                    postResponse.setAssetDetail(assetMapper.convertToResponse(assetResponse));
                    return postResponse;
                })
                .collect(Collectors.toList());


        return PageResponse.<PostResponse>builder()
                .content(posts)
                .size(posts.size())
                .totalPages(postPage.getTotalPages())
                .totalElements(postPage.getTotalElements())
                .last(postPage.isLast())
                .build();
    }

    @Override
    public List<PostResponse> findAll() {
        List<PostResponse> postResponses = postRepo.findAll().stream()
                .map(p -> {
                    AssetResponse assetResponse = assetToPostServiceBlockingStub.getAssetDetail(AssetRequest.newBuilder()
                            .setId(p.getAssetId())
                            .build());

                    UserPostInfoResponse userInfoResponse = userToPostServiceBlockingStub.getUserInfo(UserInfoRequest.newBuilder()
                            .setId(p.getUserId())
                            .build());

                    PostResponse postResponse = postMapper.convertToPostDetailResponse(p);
                    postResponse.setAssetDetail(assetMapper.convertToResponse(assetResponse));
                    postResponse.setUserInfo(userMapper.convertToResponse(userInfoResponse));
                    return postResponse;
                })
                .collect(Collectors.toList());

        return postResponses;
    }

    @Override
    public PageResponse<PostOverviewResponse> findAllByStatus(String status, String kw, String userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostOverview> postOverviews = postRepo.findAllByStatus(pageable, userId, kw, PostStatus.valueOf(status));

        List<PostOverviewResponse> data = postOverviews.get()
                .map(p -> {
                    PostOverviewResponse overviewResponse = postMapper.convertToResponse(p);
                    Content content = assetToPostServiceBlockingStub.getPoster(
                            AssetRequest.newBuilder()
                                    .setUserId(p.getUserId())
                                    .setId(p.getAssetId())
                                    .build()
                    );
                    overviewResponse.setPosterUrl(content.getUrl());
                    return overviewResponse;
                }).collect(Collectors.toList());


        return PageResponse.<PostOverviewResponse>builder()
                .content(data)
                .last(postOverviews.isLast())
                .totalPages(postOverviews.getTotalPages())
                .page(page)
                .size(postOverviews.getSize())
                .totalElements(postOverviews.getTotalElements())
                .build();
    }

    @Override
    public PageResponse<PostAdminOverviewResponse> findAllByStatus(String status, String kw, int page, int limit
            , String orderBy, String orderDirection) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.valueOf(orderDirection.toUpperCase())
                , orderBy));
        PostStatus postStatus = status != null ? PostStatus.valueOf(status) : null;
        Page<PostOverview> postOverviews = postRepo.findAllByStatus(pageable, null, kw, postStatus);

        List<PostAdminOverviewResponse> data = postOverviews.get()
                .map(p -> {
                    PostAdminOverviewResponse overviewResponse = postMapper.convertToAdminResponse(p);

                    UserPostInfoResponse userInfoResponse = userToPostServiceBlockingStub.getUserInfo(
                            UserInfoRequest.newBuilder()
                                    .setId(p.getUserId())
                                    .build()
                    );
                    overviewResponse.setUserInfo(userMapper.convertToResponse(userInfoResponse));
                    return overviewResponse;
                }).collect(Collectors.toList());


        return PageResponse.<PostAdminOverviewResponse>builder()
                .content(data)
                .last(postOverviews.isLast())
                .totalPages(postOverviews.getTotalPages())
                .page(page)
                .size(postOverviews.getSize())
                .totalElements(postOverviews.getTotalElements())
                .build();
    }

    @Override
    @Transactional
    public MessageResponse createComment(String postId, String userId, Map<String, String> content) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        Comment comment = Comment.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .content(content.get("content"))
                .build();

        post.addComment(comment);

        comment = commentRepo.save(comment);
        return MessageResponse.builder()
                .message("Successfully create comment")
                .data(Map.of(
                        "id", comment.getId(),
                        "content", comment.getContent(),
                        "createdAt", comment.getCreatedAt()
                ))
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse deleteComment(String postId, Map<String, String> content) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundExeption("Not found post"));

        List<Comment> comments = post.getComments().stream()
                .filter(c -> c.getId().equals(content.get("commentId")))
                .collect(Collectors.toList());

        if (comments.isEmpty())
            throw new ResourceNotFoundExeption("Not found comment");

        Comment comment = comments.get(0);

        if (!comment.getUserId().equals(content.get("userId")))
            throw new UnauthorizedException("Don't have permission for this resource");

        post.removeComment(comment);

        return MessageResponse.builder()
                .message("Successfully delete comment")
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    public PageResponse<CommentResponse> findCommentsByPostId(String postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepo.findAllByPostId(postId, pageable);
        List<CommentResponse> commentResponses = comments.get()
                .map(c -> {
                    CommentResponse commentResponse = CommentResponse.builder()
                            .id(c.getId())
                            .content(c.getContent())
                            .createdAt(c.getCreatedAt())
                            .build();

                    UserPostInfoResponse userInfoResponse = userToPostServiceBlockingStub.
                            getUserInfo(UserInfoRequest.newBuilder()
                                    .setId(c.getUserId())
                                    .build());

                    commentResponse.setUserInfo(CommentResponse.UserInfo.builder()
                            .firstName(userInfoResponse.getFirstName())
                            .lastName(userInfoResponse.getLastName())
                            .avtUrl(userInfoResponse.getAvtUrl())
                            .id(userInfoResponse.getId())
                            .build());

                    return commentResponse;

                })
                .collect(Collectors.toList());

        return PageResponse.<CommentResponse>builder()
                .content(commentResponses)
                .size(comments.getSize())
                .last(comments.isLast())
                .totalElements(comments.getTotalElements())
                .page(comments.getNumber())
                .totalPages(comments.getTotalPages())
                .build();
    }

    @Override
    public List<StatusCountResponse> countStatusPost(String userId) {
        List<StatusCount> counts = postRepo.countStatusPost(userId);

        return counts.stream()
                .map(c -> StatusCountResponse.builder()
                        .count(c.getCount())
                        .name(c.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}

