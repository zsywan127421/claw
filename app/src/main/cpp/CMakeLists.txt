cmake_minimum_required(VERSION 3.22.1)
project(termux_terminal)

add_library(termux SHARED termux_pty.cpp)
find_library(log-lib log)
find_library(android-lib android)
target_link_libraries(termux ${log-lib} ${android-lib})
