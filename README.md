Processing data using the GESTALT pipeline
=============

The GESTALT pipeline is run in two parts. The first processes raw amplicon sequencing reads into GESTALT barcode calls. This step may involve unique molecular identifer (UMI) collapsing, alignment, and event calling.

Before you begin
=============
Things you need to have:
- reference file (fasta): this should be a single entry fasta file, that contains a standard header line that starts with a ">", followed by the sequence of the amplicon. This can be split over many lines if you prefer.
- A cutsite file. This should have the name <reference.fa>.cutSites, where the <reference.fa> is the name of your reference. This describes where the cutsites occur within the amplicon. See the example_data directory for an example version. Positions are relative to the start of the reference.
- A primers file. A file containing the invariant sequences that are expected to be present 