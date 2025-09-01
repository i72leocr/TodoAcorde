cmake_minimum_required(VERSION 3.10.2)

project("TodoAcorde")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Añadir la biblioteca fft_processing que incluye kiss_fft y fft_processing
add_library(fft_processing SHARED
        cpp/kiss_fft.c
        cpp/kiss_fftr.c
        cpp/fft_processing.cpp)

# Incluir directorio para los encabezados
include_directories(cpp)

# Encontrar la biblioteca de registro de Android
find_library(log-lib log)

# Enlazar la biblioteca de registro con fft_processing
target_link_libraries(fft_processing ${log-lib})
