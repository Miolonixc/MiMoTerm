cmake_minimum_required(VERSION 3.22.1)
project("mimoterm")

add_library(
    mimoterm
    SHARED
    pty.c
)

find_library(log-lib log)

target_link_libraries(
    mimoterm
    ${log-lib}
)
