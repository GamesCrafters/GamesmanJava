#!/usr/bin/python

import subprocess
import argparse

parser = argparse.ArgumentParser(description='Validates Connect 4 database files. Database files should be in the format "*wh*", where w and h are the width and height of the connect 4 board, respectively.')

parser.add_argument(dest='file_or_directory',
        help='path to a database file or a directory containing such files.')


def main():
    pass

if __name__ == "__main__":
    main()
