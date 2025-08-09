package com.tanle.tland.post_service.mapper.decorator;


import com.tanle.tland.post_service.mapper.AssetMapper;
import com.tanle.tland.post_service.response.AssetDetailResponse;
import com.tanle.tland.user_serivce.grpc.AssetResponse;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.stream.Collectors;

@NoArgsConstructor
public abstract class AssetMapperDecorator implements AssetMapper {
    @Autowired
    private AssetMapper assetMapper;

    @Override
    public AssetDetailResponse convertToResponse(AssetResponse assetResponse) {
        AssetDetailResponse response = assetMapper.convertToResponse(assetResponse);

        response.setDimension(assetResponse.getDimensionList());

        response.setOtherInfo(assetResponse.getOtherInfoList().toArray(new String[0]));
        response.setContents(
                assetResponse.getContentListList().stream()
                        .map(c -> AssetDetailResponse.Content.builder()
                                .url(c.getUrl())
                                .id(c.getId())
                                .name(c.getName())
                                .duration(c.getDuration())
                                .type(c.getType())
                                .build())
                        .collect(Collectors.toList())
        );
        return response;
    }
}
