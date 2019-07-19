cmake_minimum_required(VERSION 3.6)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-error=format-security ")
#set(CMAKE_INTERPROCEDURAL_OPTIMIZATION TRUE)
#set(CMAKE_ANDROID_STL_TYPE  c++_static)
#if(GNU)
#    add_compile_options(-Wall -Wno-unused-parameter)
#    endif()

add_subdirectory(quickjs)
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)
add_library( # Sets the name of the library.
        quickjspp

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        quickjs.cpp)
target_link_libraries(quickjspp quickjs ${log-lib})
