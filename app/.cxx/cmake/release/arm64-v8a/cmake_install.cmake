<<<<<<< HEAD
# Install script for directory: /Users/minj/Documents/AndroidProject/DrawingTogether/app/src/main/cpp
=======
# Install script for directory: D:/DrawingTogether0616/DrawingTogether/app/src/main/cpp
>>>>>>> 3e197793e3f110e598dfb21e492cdc5cb8e3ae19

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/usr/local")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Release")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

if(CMAKE_INSTALL_COMPONENT)
  set(CMAKE_INSTALL_MANIFEST "install_manifest_${CMAKE_INSTALL_COMPONENT}.txt")
else()
  set(CMAKE_INSTALL_MANIFEST "install_manifest.txt")
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
<<<<<<< HEAD
file(WRITE "/Users/minj/Documents/AndroidProject/DrawingTogether/app/.cxx/cmake/release/arm64-v8a/${CMAKE_INSTALL_MANIFEST}"
=======
file(WRITE "D:/DrawingTogether0616/DrawingTogether/app/.cxx/cmake/release/arm64-v8a/${CMAKE_INSTALL_MANIFEST}"
>>>>>>> 3e197793e3f110e598dfb21e492cdc5cb8e3ae19
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
