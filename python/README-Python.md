# HKID Utilities for Python

The Python package calls the Rust HKID number library. It targets standard
CPython 3.11 and later using the stable ABI. The wheel build targets are
Windows x86-64, macOS on Apple silicon and Intel, and Linux with glibc 2.28
or later on x86-64 and ARM64.

## Quick start

Install the prebuilt wheel from [PyPI](https://pypi.org/project/hkid-utils/).

~~~shell
pip install hkid-utils
~~~

## Example

~~~python
from hkid_utils import check_digit, format_number, is_valid

# Validation
print(is_valid("A123456(3)"))  # True
print(is_valid("A123456(4)"))  # False (expected check digit 3)

# Formatting
number = "A123456"
print(format_number(number))  # A123456(3)
print(check_digit(number))    # 3
~~~

The check digit can also be a letter. For example, `check_digit("a000002")`
returns `"A"`. Fullwidth input such as `ａ１２３４５６` formats as `A123456(3)`.

All three functions accept a string with a one- or two-letter prefix, six
digits and an optional check digit. A supplied digit must match. For invalid
text, `is_valid` returns `False`, and `check_digit` and `format_number` raise
`ValueError`. A missing number (`None`) also validates as `False`, while
`check_digit(None)` and `format_number(None)` raise `TypeError`. Other
non-string inputs raise `TypeError` for all three functions.

## Install a CI wheel

On GitHub, open a successful **Python CI** run under **Actions**. Download the
`.whl` for your operating system and CPU from **Artifacts**, then run
`python -m pip install <path-to-wheel>` with the downloaded file's path.
CI installs each wheel on its native runner and runs the number tests on both
CPython 3.11 and the latest stable CPython.

## Build and test locally

From the repository root, use an active Python virtual environment with the
repository's Rust toolchain and a native linker to build the wheel.

~~~shell
python -m pip wheel ./python --no-deps --wheel-dir target/wheels
~~~

In a fresh virtual environment, install the local wheel and run the tests
against the shared number fixtures.

~~~shell
python -m pip install --no-index --find-links target/wheels hkid-utils
python -m unittest discover -s python/tests -v
~~~
