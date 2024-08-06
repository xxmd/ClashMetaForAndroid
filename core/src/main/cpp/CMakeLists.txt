cmake_minimum_required(VERSION 3.0)
project(clash-bridge C)

set(CMAKE_POSITION_INDEPENDENT_CODE on)
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O3")
set(JNILIBS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../jnilibs/${CMAKE_ANDROID_ARCH_ABI}")

include_directories("${JNILIBS_DIR}")

link_directories("${JNILIBS_DIR}")

add_library(bridge SHARED main.c jni_helper.c bridge_helper.c)
target_link_libraries(bridge log clash)
