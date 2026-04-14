package com.demo.controller;

import com.demo.domain.constant.error.CommonExceptions;
import com.demo.domain.dto.BaseResponse;
import com.demo.domain.dto.http.resonse.OssBatchDeleteResponse;
import com.demo.domain.dto.http.resonse.OssBatchUploadResponse;
import com.demo.domain.dto.http.resonse.OssFileContentUpdateResponse;
import com.demo.domain.dto.http.resonse.OssFileNameUpdateResponse;
import com.demo.domain.dto.http.resonse.OssUrlListResponse;
import com.demo.domain.dto.http.resonse.OssUserBucketFileIdsResponse;
import com.demo.domain.dto.http.resonse.OssUserBucketFileItemListResponse;
import com.demo.domain.dto.http.resonse.OssUserBucketFileItemRow;
import com.demo.domain.dto.http.resonse.OssUserBucketFileUrlsResponse;
import com.demo.domain.dto.http.resonse.OssUserBucketListResponse;
import com.demo.domain.dto.http.resonse.OssUserDeleteAllResponse;
import com.minio.config.MinioConfig;
import com.minio.domain.bo.OssBucketFileItemBo;
import com.minio.domain.bo.BatchUploadResult;
import com.minio.domain.bo.UploadItemResult;
import com.minio.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/oss")
public class OssController {

    private final OssService ossService;
    private final MinioConfig minioConfig;

    @PostMapping("/upload/batch")
    public BaseResponse<OssBatchUploadResponse> batchUpload(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "bucketName", required = false) String bucketName,
            @RequestParam("files") List<MultipartFile> files
    ) {
        if (userId == null || CollectionUtils.isEmpty(files)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        String targetBucket = StringUtils.hasText(bucketName) ? bucketName : minioConfig.globalOssBucket();
        BatchUploadResult uploadResult = ossService.uploadFiles(files, userId, targetBucket);
        OssBatchUploadResponse response = new OssBatchUploadResponse();
        response.setUserId(userId);
        response.setBucketName(targetBucket);
        response.setSuccessCount(uploadResult.getSuccessCount());
        response.setFailCount(uploadResult.getFailCount());
        response.setItems(uploadResult.getItems());
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/file/name/update")
    public BaseResponse<OssFileNameUpdateResponse> updateFileName(
            @RequestParam("fileId") Long fileId,
            @RequestParam("newFileName") String newFileName
    ) {
        if (fileId == null || !StringUtils.hasText(newFileName)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        boolean updated = ossService.updateFileNameByFileId(fileId, newFileName);
        OssFileNameUpdateResponse response = new OssFileNameUpdateResponse();
        response.setFileId(fileId);
        response.setNewFileName(newFileName);
        response.setUpdated(updated);
        response.setMessage(updated ? "ok" : "file not found or update failed");
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/file/content/update")
    public BaseResponse<OssFileContentUpdateResponse> updateFileContent(
            @RequestParam("fileId") Long fileId,
            @RequestParam("file") MultipartFile file
    ) {
        if (fileId == null || file == null || file.isEmpty()) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        UploadItemResult item = ossService.updateFileContentByFileId(fileId, file);
        OssFileContentUpdateResponse response = new OssFileContentUpdateResponse();
        response.setFileId(fileId);
        response.setOriginFileName(item.getOriginFileName());
        response.setUrl(item.getUrl());
        response.setUpdated(item.isSuccess());
        response.setMessage(item.getMessage());
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/file/url/list")
    public BaseResponse<OssUrlListResponse> getUrlList(
            @RequestParam("fileIdList") List<Long> fileIdList
    ) {
        if (fileIdList == null) {
            fileIdList = new ArrayList<>();
        }
        List<String> urlList = ossService.getFileUrlsByFileIds(fileIdList);
        OssUrlListResponse response = new OssUrlListResponse();
        response.setFileIdList(fileIdList.stream().map(String::valueOf).toList());
        response.setUrlList(urlList);
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/file/delete/batch")
    public BaseResponse<OssBatchDeleteResponse> batchDelete(
            @RequestParam("fileIdList") List<Long> fileIdList
    ) {
        if (CollectionUtils.isEmpty(fileIdList)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        int successCount = ossService.deleteFilesByFileIds(fileIdList);
        OssBatchDeleteResponse response = new OssBatchDeleteResponse();
        response.setFileIdList(fileIdList.stream().map(String::valueOf).toList());
        response.setSuccessCount(successCount);
        response.setFailCount(fileIdList.size() - successCount);
        response.setMessage(response.getFailCount() == 0 ? "ok" : "partial success");
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/user/bucket/list")
    public BaseResponse<OssUserBucketListResponse> listBucketsByUserId(
            @RequestParam("userId") String userIdStr
    ) {
        Long userId = parseUserIdParam(userIdStr);
        if (userId == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        List<String> buckets = ossService.listBucketNamesByUserId(userId);
        OssUserBucketListResponse response = new OssUserBucketListResponse();
        response.setUserId(String.valueOf(userId));
        response.setBucketNameList(buckets);
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/user/bucket/file/id/list")
    public BaseResponse<OssUserBucketFileIdsResponse> listFileIdsByUserAndBucket(
            @RequestParam("userId") String userIdStr,
            @RequestParam("bucketName") String bucketName
    ) {
        Long userId = parseUserIdParam(userIdStr);
        if (userId == null || !StringUtils.hasText(bucketName)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        List<Long> ids = ossService.listFileIdsByUserIdAndBucket(userId, bucketName);
        OssUserBucketFileIdsResponse response = new OssUserBucketFileIdsResponse();
        response.setUserId(String.valueOf(userId));
        response.setBucketName(bucketName.trim());
        response.setFileIdList(ids.stream().map(String::valueOf).toList());
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/user/bucket/file/url/list")
    public BaseResponse<OssUserBucketFileUrlsResponse> listFileUrlsByUserAndBucket(
            @RequestParam("userId") String userIdStr,
            @RequestParam("bucketName") String bucketName
    ) {
        Long userId = parseUserIdParam(userIdStr);
        if (userId == null || !StringUtils.hasText(bucketName)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        List<String> urls = ossService.listFileUrlsByUserIdAndBucket(userId, bucketName);
        OssUserBucketFileUrlsResponse response = new OssUserBucketFileUrlsResponse();
        response.setUserId(String.valueOf(userId));
        response.setBucketName(bucketName.trim());
        response.setUrlList(urls);
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/user/bucket/file/item/list")
    public BaseResponse<OssUserBucketFileItemListResponse> listFileItemsByUserAndBucket(
            @RequestParam("userId") String userIdStr,
            @RequestParam("bucketName") String bucketName
    ) {
        Long userId = parseUserIdParam(userIdStr);
        if (userId == null || !StringUtils.hasText(bucketName)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        List<OssBucketFileItemBo> rows = ossService.listFileItemsByUserIdAndBucket(userId, bucketName);
        OssUserBucketFileItemListResponse response = new OssUserBucketFileItemListResponse();
        response.setUserId(String.valueOf(userId));
        response.setBucketName(bucketName.trim());
        response.setItems(rows.stream().map(bo -> {
            OssUserBucketFileItemRow row = new OssUserBucketFileItemRow();
            row.setFileId(String.valueOf(bo.getFileId()));
            row.setOriginFileName(bo.getOriginFileName());
            row.setUrl(bo.getUrl());
            return row;
        }).toList());
        return BaseResponse.getResponseEntitySuccess(response);
    }

    @PostMapping("/user/delete/all")
    public BaseResponse<OssUserDeleteAllResponse> deleteAllByUserId(
            @RequestParam("userId") Long userId
    ) {
        if (userId == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        int totalCount = ossService.countFilesByUserId(userId);
        int successCount = ossService.deleteAllFilesByUserId(userId);
        OssUserDeleteAllResponse response = new OssUserDeleteAllResponse();
        response.setUserId(userId);
        response.setTotalCount(totalCount);
        response.setSuccessCount(successCount);
        response.setFailCount(Math.max(0, totalCount - successCount));
        response.setMessage(response.getFailCount() == 0 ? "ok" : "partial success");
        return BaseResponse.getResponseEntitySuccess(response);
    }

    private static Long parseUserIdParam(String userIdStr) {
        if (!StringUtils.hasText(userIdStr)) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
