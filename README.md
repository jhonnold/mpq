<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Contributors][contributors-shield]][contributors-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]

<!-- PROJECT LOGO -->
<br />
<p align="center">
  <h3 align="center">MPQ Archive Parser</h3>
  <p align="center">
    Kotlin/Java library to parse MPQ archives used by <a href="https://www.blizzard.com">Blizzard</a> games.
    Written mostly for Starcraft II replay files.
  </p>
</p>

<!-- TABLE OF CONTENTS -->
## Table of Contents

* [Getting Started](#getting-started)
  * [Gradle](#gradle)
  * [Maven](#maven)
* [Usage](#usage)
* [Contributing](#contributing)
* [License](#license)
* [Contact](#contact)
* [Acknowledgements](#acknowledgements)


<!-- GETTING STARTED -->
## Getting Started

Follow the steps below to use this within project.

#### Gradle
```gradle
implementation group: 'me.honnold', name: 'mpq', version: ...
```

#### Maven
```xml
<dependency>
    <groupId>me.honnold</groupId>
    <artifactId>mpq</artifactId>
    <version>...</version>
</dependency>
```

<!-- USAGE EXAMPLES -->
## Usage

Import the base `Archive` class and supply a path to the MPQ file.

```java
import me.honnold.mpq.Archive;

public class Example {
    public static void main(String[] args) {
        Archive archive = new Archive(/* Path to file */);
       
        // ...
    }
}
```


<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to be learn, inspire, and create. 
Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request



<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE` for more information.



<!-- CONTACT -->
## Contact

Jay Honnold

Project Link: [https://github.com/jhonnold/mpq-archive-parser](https://github.com/jhonnold/mpq-archive-parser)


<!-- ACKNOWLEDGEMENTS -->
## Acknowledgements
* [mpyq](https://github.com/eagleflo/mpyq)
* [Scelight](https://github.com/icza/scelight)
* [MPQ Archives](http://www.zezula.net/en/mpq/main.html)


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/jhonnold/mpq-archive-parser?style=flat-square
[contributors-url]: https://github.com/jhonnold/mpq-archive-parser/graphs/contributors
[issues-shield]: https://img.shields.io/github/issues/jhonnold/mpq-archive-parser.svg?style=flat-square
[issues-url]: https://github.com/jhonnold/mpq-archive-parser/issues
[license-shield]: https://img.shields.io/github/license/jhonnold/mpq-archive-parser.svg?style=flat-square
[license-url]: https://github.com/jhonnold/mpq-archive-parser/blob/master/LICENSE