cmake_minimum_required(VERSION 3.4.1)

set(THIRD_PARTY_LIB_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../libs)

add_library(faac STATIC IMPORTED)
set_target_properties(faac PROPERTIES
        IMPORTED_LOCATION
        ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libfaac.a)

add_library(x264 STATIC IMPORTED)
set_target_properties(x264 PROPERTIES
        IMPORTED_LOCATION
        ${THIRD_PARTY_LIB_DIR}/${CMAKE_ANDROID_ARCH_ABI}/libx264.a)

add_library(ffmpeg SHARED IMPORTED)
set_target_properties(ffmpeg PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/libffmpeg.so)

add_subdirectory(rtmp)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/live)
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

if(${CMAKE_ANDROID_ARCH_ABI} MATCHES "armeabi-v7a")
    include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include/armeabi-v7a/libavcodec)
    include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include/armeabi-v7a/libavutil)
elseif(${CMAKE_ANDROID_ARCH_ABI} MATCHES "arm64-v8a")
    include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include/arm64-v8a/libavcodec)
    include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include/arm64-v8a/libavutil)
endif()

add_library(aar_live
        SHARED
        live/AudioStream.cpp
        live/VideoStream.cpp
        live/RtmpPusher.cpp
        live/ff_rtmp_pusher.cpp
        live/ffmpeg_pusher_jni.cpp)

find_library(log-lib log)

target_link_options(aar_live PRIVATE "-Wl,-z,max-page-size=16384")

target_link_libraries(aar_live
        x264
        faac
        rtmp
        ffmpeg
        -landroid
        -ljnigraphics
        -lOpenSLES
        ${log-lib})
