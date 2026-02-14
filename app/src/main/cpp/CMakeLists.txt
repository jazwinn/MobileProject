cmake_minimum_required(VERSION 3.22.1)

project("image_utils")

add_library(
        image_utils
        SHARED
        image_utils.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        image_utils
        ${log-lib})
