import 'package:dio/dio.dart';
import 'package:retrofit/retrofit.dart';

import '../constant/network_constant.dart';
import '../domain/dto/base_response.dart';
import '../domain/dto/req/user_login_request.dart';
import '../domain/dto/req/user_password_update_request.dart';
import '../domain/dto/req/user_test_req.dart';
import '../domain/dto/req/user_token_verify_request.dart';
import '../domain/dto/resp/oss_batch_delete_response.dart';
import '../domain/dto/resp/oss_batch_upload_response.dart';
import '../domain/dto/resp/oss_file_content_update_response.dart';
import '../domain/dto/resp/user_auth_response.dart';
import '../domain/dto/resp/user_password_update_response.dart';
import '../domain/dto/resp/user_test_resp.dart';
import '../domain/dto/resp/user_token_verify_response.dart';
import '../domain/dto/resp/oss_user_bucket_file_item_list_response.dart';
import '../domain/dto/resp/oss_user_bucket_file_ids_response.dart';
import '../domain/dto/resp/oss_user_bucket_file_urls_response.dart';
import '../domain/dto/resp/oss_user_bucket_list_response.dart';

part 'api_request.g.dart';

@RestApi(baseUrl: NetworkConstant.baseUrl)
abstract class ApiRequest {
  factory ApiRequest(Dio dio, {String baseUrl}) = _ApiRequest;

  // --- Demo 测试接口（保留） ---
  @POST('/test/network/register')
  Future<BaseResponse<UserTestResp>> registerTest(@Body() UserTestReq req);

  @GET('/test/network/resetToken')
  Future<BaseResponse<UserTestResp>> resetToken(@Query('account') String account);

  // --- 用户 Auth（对齐 Android ApiRequest） ---
  @POST('/user/login')
  Future<BaseResponse<UserAuthResponse>> userLogin(@Body() UserLoginRequest request);

  @POST('/user/token/verify')
  Future<BaseResponse<UserTokenVerifyResponse>> userVerifyAccessToken(
    @Body() UserTokenVerifyRequest request,
  );

  @POST('/user/password/update')
  Future<BaseResponse<UserPasswordUpdateResponse>> userUpdatePassword(
    @Body() UserPasswordUpdateRequest request,
  );

  @POST('/user/register')
  @MultiPart()
  Future<BaseResponse<UserAuthResponse>> userRegister(
    @Part() String account,
    @Part() String password,
    @Part() String name,
  );

  // --- OSS（对齐 Android ApiRequest） ---
  @POST('/oss/user/bucket/list')
  @MultiPart()
  Future<BaseResponse<OssUserBucketListResponse>> ossUserBucketList(
    @Part() String userId,
  );

  @POST('/oss/user/bucket/file/id/list')
  @MultiPart()
  Future<BaseResponse<OssUserBucketFileIdsResponse>> ossUserBucketFileIdList(
    @Part() String userId,
    @Part() String bucketName,
  );

  @POST('/oss/user/bucket/file/url/list')
  @MultiPart()
  Future<BaseResponse<OssUserBucketFileUrlsResponse>> ossUserBucketFileUrlList(
    @Part() String userId,
    @Part() String bucketName,
  );

  @POST('/oss/user/bucket/file/item/list')
  @MultiPart()
  Future<BaseResponse<OssUserBucketFileItemListResponse>> ossUserBucketFileItemList(
    @Part() String userId,
    @Part() String bucketName,
  );

  @POST('/oss/upload/batch')
  @MultiPart()
  Future<BaseResponse<OssBatchUploadResponse>> ossBatchUpload(
    @Part() String userId,
    @Part(name: 'bucketName') String? bucketName,
    @Part(name: 'files') MultipartFile files,
  );

  @POST('/oss/file/delete/batch')
  Future<BaseResponse<OssBatchDeleteResponse>> ossBatchDelete(@Body() FormData form);

  @POST('/oss/file/content/update')
  @MultiPart()
  Future<BaseResponse<OssFileContentUpdateResponse>> ossUpdateFileContent(
    @Part() String fileId,
    @Part(name: 'file') MultipartFile file,
  );
}
