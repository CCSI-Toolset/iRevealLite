# iREVEAL
Automate tool to create reduced order model and export solution for CFD models

## Getting Started
See installaton and user guide documents in the [documentation](docs) subdirectory.

This software has been compiled and tested on Windows 7 professional

### Pre-requisites
Your environment must have the following tools installed.
The build has be tested with the following versions. Use other
versions at your own risk.

+ Ant 1.9.6
+ Git Bash for windows
+ Java 1.8
+ 7z

### Build and Package
After installing the tools above run the Git Bash program.
Executing the commands below tocompile the source and 
and package the output


```
git clone https://github.com/CCSI-Toolset/iREVEAL.git
cd iREVEAL
start make.bat

```

## Authors

* Poorva Sharma
* Khushbu Agarwal

See also the list of [contributors](../../contributors) who participated in this project.

## Development Practices

* Code development will be peformed in a forked copy of the repo. Commits will not be 
  made directly to the repo. Developers will submit a pull request that is then merged
  by another team member, if another team member is available.
* Each pull request should contain only related modifications to a feature or bug fix.  
* Sensitive information (secret keys, usernames etc) and configuration data 
  (e.g database host port) should not be checked in to the repo.
* A practice of rebasing with the main repo should be used rather that merge commmits.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, 
[releases](../../releases) or [tags](../..//tags) on this repository. 

## License & Copyright

See [LICENSE.md](LICENSE.md) file for details
