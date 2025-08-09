package com.tanle.tland.search_service.mapper.decorator;

import com.google.apps.card.v1.Image;
import com.google.protobuf.ProtocolStringList;
import com.tanle.tland.search_service.entity.AssetDocument;
import com.tanle.tland.search_service.entity.enums.ContentType;
import com.tanle.tland.search_service.mapper.AssetMapper;
import com.tanle.tland.user_serivce.grpc.AssetResponse;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AssetMapperDecorator implements AssetMapper {
    @Autowired
    private AssetMapper assetMapper;


    @Override
    public AssetDocument convertToDocument(AssetResponse response) {
        AssetDocument assetDocument = assetMapper.convertToDocument(response);
        if (response.getContentListList() != null) {
            AssetDocument.MediaDocument mediaDocument = response.getContentListList().stream()
                    .filter(c -> ContentType.IMAGE.name().equals(c.getType()))
                    .findFirst()
                    .map(c -> AssetDocument.MediaDocument.builder()
                            .url(c.getUrl())
                            .id(c.getId())
                            .build())
                    .get();
            assetDocument.setMedia(mediaDocument);
        }

        assetDocument.setDimension(response.getDimensionList().stream().mapToInt(Integer::intValue).toArray());
        assetDocument.setOtherInfo(response.getOtherInfoList()
                .stream()
                .map(String::valueOf)
                .toArray(String[]::new));


        return assetDocument;
    }
}
