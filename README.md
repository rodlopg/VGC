# Video Client Generator

## Dear Dr. Jafet and Mr. Moya

<div style="background-color: #fff3cd; padding: 15px; border-left: 5px solid #ffeeba; margin-bottom: 20px;">
<strong># IMPORTANT</strong><br>
For the following classes: <code>FFMPEG</code>, <code>EXIF</code>, <code>OpenAI</code> please search the first line of code of each one for a constant where you have to replace the value with your respective path/API KEY for the code to properly execute and let us (lettuce, lol) be happy.
</div>

## Overview
The **Video Client Generator** is a modular tool designed to process and convert video files using a pipeline-based workflow. It integrates FFmpeg functionalities within a structured, object-oriented architecture to provide efficient and customizable media processing.

## Project Flow
The project follows a **component-based system**, where different classes interact to construct, configure, and execute video processing commands. The overall flow can be described as follows:

1. **User Input & Configuration**
   - The user provides video files and processing parameters.
   - Inputs are parsed and validated.
   
2. **Command Generation**
   - Classes construct FFmpeg commands using reusable components.
   - The `Pipeline` class orchestrates these command sequences.
   
3. **Execution & Processing**
   - The command pipeline is executed to process the video files.
   - The system handles errors and ensures optimal execution.
   
4. **Output Generation**
   - Processed video files are generated and stored in the specified location.
   - Logging captures execution details for debugging and optimization.

---

## Key Classes & Their Interactions

### 🔧 `Utils`
> Provides utility functions for constructing FFmpeg command-line arguments.  
> Includes methods for input/output handling, format selection, and parameter customization.

### 💻 `CMD`
> Normalizes file paths to ensure compatibility across different operating systems.  
> Concatenates command-line arguments for streamlined execution.  
> This class handles everything that has to do with the command terminal. Static methods like `run()` and `expect()` are in charge of sending the commands via a process builder and running them on the CMD.

### 🚀 `Pipeline`
> Manages the flow of command execution.  
> Utilizes functional programming techniques to apply transformations to commands.  
> *This was the part that I am the most hyped about. It is not a complex Pipeline or anything, but it is mine. Basically, what it does, as for now, is store a static method `biLambda()` that receives a `List<>` of `String[]` functions and also a `Function` object as parameters. This way, I can apply the `Function` object to all the `String[]` functions on the `List<>` object and return a single `String[]` object. This is enormously good for building the whole program architecture because instead of just hardcoding the commands, I can just code different functions, make them as complex as I want, and just join them by calling `Pipeline.biLambda();`. For me, this is super cool for modularity, scalability, reusability, refactoring, understandability, simplicity, and basically any word that ends with "-ity" (except city).*

### 📝 `Format`
> Stores predefined values for encoding presets, bitrates, codecs, and pixel formats.  
> Ensures consistency when defining FFmpeg parameters.

### 🎥 `FFMPEG`
> Defines the execution path of the FFmpeg binary.  
> Allows for dynamic configuration and updates.  
> *Also, for further facilitating the use of the different options that FFmpeg has, I decided to make two more classes (`Format` and `Filter`) to make the management and use of this program easier, scalable, and as reusable as possible.*  
> *In both `Format` and `Filter` classes, what was done was create a repository of `String` arrays and static methods that would help facilitate the management of options like choosing a codec, selecting bitrates, choosing pixel formats, creating simple filters, creating complex filters, and more. Obviously, using encapsulation and also adding a bit of processing to help the developer with the use of these options: for instance, the use of the module operator `%` to make this part foolproof and also receiving only `int` values as a way to mitigate the use of `String` objects and reduce possible errors.*

### 🔍 `EXIF`
> Handles metadata extraction from media files using ExifTool  
> Provides methods to get width, height, creation date, duration, MIME type, and codec information  
> Includes internal normalization for consistent output formats  
> Used by the system to gather technical specifications about media files

### 🤖 `OpenAI`
> Integrates with multiple OpenAI APIs including DALL-E, GPT-4 Vision, and Text-to-Speech  
> Generates images from text prompts using DALL-E  
> Creates detailed descriptions of images using GPT-4 Vision  
> Converts text to speech with configurable voice options  
> Handles both URL-based and base64-encoded image inputs

### 🖥️ `GClient`
> The graphical user interface for the application  
> Provides visual controls for all system functionalities  
> Displays processing progress and results  
> Integrates with both FFmpeg processing and AI features

---

## How the Classes Work Together
```mermaid
graph LR
    GClient --> Pipeline
    GClient --> OpenAI
    OpenAI --> FFMPEG
    Pipeline --> CMD
    Pipeline --> Utils
    Utils --> EXIF
    CMD --> FFMPEG
    FFMPEG --> Format
```

## Conclusion
The **Video Client Generator** provides a structured, modular approach to video processing using FFmpeg. By leveraging an object-oriented architecture, the project ensures **scalability, flexibility, and maintainability**, making it a powerful tool for video encoding and format conversion.

---

#### Done while listening to my inner monologue I guess...
- *Rodrigo López Gómez*