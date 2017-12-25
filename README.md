# Single-cell GESTALT pipeline

This Docker image processes GESTALT barcode sequencing data. Reads are merged by UMI, aligned to the reference file, and aggregated into plots describing the overall editing, as well as trees describing the relative lineage relationships within your data. 


# Processing data using the GESTALT pipeline

## Before you begin
Things you need to have:
* Reference files. See examples in /app/references/ in the Docker container
- **A Docker installation**. Follow the documentation here: https://docs.docker.com/engine/installation/
- **reference file (fasta)**: this should be a single entry fasta file, that contains a standard header line that starts with a ">", followed by the sequence of the amplicon. This can be split over many lines if you prefer.
- **A cutsite file**. This should have the name <reference.fa>.cutSites, where the <reference.fa> is the name of your reference. This describes where the cutsites occur within the amplicon. See the example_data directory for an example version. Positions are relative to the start of the reference.
- **A primers file**, named <reference.fa>.primers. A file containing the invariant sequences that are expected to be present at both ends of the amplicon. This file is two lines, a primer on each line oriented with the reference. This is used to filter out non-specific PCR products that may be present in your sequencing run. This filtering is optional, and if you don't use this you can just put any sequences in this file.
- **A processing tearsheet**, describing your samples. See the example 'sample_tearsheet.tsv' in the example_data file.

## Install the Docker container



Downloading and starting the Docker container

