project(quickjs LANGUAGES C)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-error=format-security ")
file(STRINGS VERSION version)

set(quickjs_src quickjs.c libunicode.c libregexp.c cutils.c quickjs-libc.c)
set(quickjs_def CONFIG_VERSION="${version}" _GNU_SOURCE)

option(QUICKJS_BIGNUM "Compile BigNum support" ON)

if (QUICKJS_BIGNUM)
    list(APPEND quickjs_src libbf.c)
    list(APPEND quickjs_def CONFIG_BIGNUM)
endif ()
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

add_library(quickjs ${quickjs_src})
target_compile_definitions(quickjs PRIVATE ${quickjs_def})
add_library(toast Toast.cpp)
target_link_libraries(toast ${log-lib})
target_link_libraries(quickjs toast)

if (UNIX)
    target_link_libraries(quickjs ${CMAKE_DL_LIBS} m)
endif ()
