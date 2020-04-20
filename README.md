# Single-cell GESTALT pipeline

This pipeline processes 2nd-generation sequencing of GESTALT lineage tracing barcodes. You can either setup the tool manually (following the directions in the Docker file), or use the Docker installation option. The pipeline merges reads by UMI, aligned the resulting consensus to a reference file, and aggregates results into plots describing the overall editing, as well as trees describing the relative lineage relationships within your data. *This code is a moving target, and may contain buggy implementations or changing code*. I will try to keep individual releases as stable, tested implementations. The current V1.0 release should be used for anyone but those needing the absolute latest code.


# Processing data using the GESTALT pipeline

## Before you begin
Things you need to have:
* **If you're using Docker, then a Docker installation**. Follow the documentation here: https://docs.docker.com/engine/installation/. If the pipeline is already installed locally then there's no need. If 

Then download the docker container:
```
docker pull aaronmck/single_cell_gestalt:stable
```

Run and connect to the container, remapping port 80 of the container to port 8080 on your local machine:
```
 docker run -it -p 8080:80 aaronmck/single_cell_gestalt:stable /bin/bash
```

# Setup an individual run

* Reference files. See examples in /app/references/ in the Docker container
  - **reference file (fasta)**: this should be a single entry fasta file containing a standard header line that starts with a ">", followed by the name of the sequence (totally arbritrary). The second line is the reference sequence, which can be split over many lines if you prefer.
  - **A cutsite file**. This should have the same name as the reference file, with the cutSites extension ( <reference.fa>.cutSites, where the <reference.fa> is the name of your reference). This describes where CRISPR/Cas9 binds to the sequence and makes a cut. There are three columns, the first is the sequence of the CRISPR target, the second is the position of the start of that target (5'), and the third is the position of the cutsite (17 basepairs downstream from the start of the target for Cas9). See the example_data directory for an example version. Positions are relative to the start of the reference.
  - **A primers file**, This file is named <reference.fa>.primers. It contains the invariant sequences that are expected to be present at both ends of the amplicon used to PCR-up the amplicon from the background. This file is two lines, a primer on each line **oriented** with the reference. This is used to filter out non-specific PCR products that may be present in your sequencing run. This filtering is optional (you can use both ends, either, or none), and will depend on if this was done with PCR (both ends can be used) or with single-cell approaches where just one end is relevant. 

* **A processing tearsheet**, describing your samples. See the example 'sample_tearsheet.tsv' in the /app/sc_GESTALT/tear_sheet_examples directory. This file can be editing in Excel or a text editor and saved as a tab-delimited file. The columns are as follows:
   - sample: used to create sample-specific output files
   - umi: Do the amplicons have UMIs? These are sequences that uniquely identify  a unique DNA fragment which was amplified by PCR.
   - reference: What reference file (.fa) to use for this sample. This reference should have the cutSites and primers files along-side (in the same directory).
   - output.dir: where to put the output on the filesystem. This directory should exist. A sub-directory matching the sample name above will be created within this parent directory
   - fastq1: The fastq.gz file containing the first reads.
   - fastq2: Second read, can be set to NA if there isn't a second read file	
   - barcode.fastq1: barcode (index) file 1
   - barcode.fastq2: barcode (index) file 2
   - barcode1: If we want to split out this sample from the fastq files, this sequence should match what's in the barcode.fastq1 file. Can be set to ALL to include all reads
   - barcode2: If we want to split out this sample from the fastq files, this sequence should match what's in the barcode.fastq2 file. Can be set to ALL to include all reads
  
Again, you can save this to a tab-delimited file on the same filesystem you plan to run the pipeline on.

### Setup a run script

The run script specifies what parameters should be used with the tearsheet above, and executes the full processing pipeline. The idea is to separate what's run (the samples in the tearsheet above) from how they're run (this script). Generally this is run as a bash or shell script that looks like the following. Items with brackets ```<like_so>``` are paths that are specific to your installation:

```
java -Xmx4g \
    -jar <path_to_queue.jar> \
    -S <pipeline_path_plus>/CRISPR_analysis_PE_V2.scala  \
    -i <your_tearsheet_path>/crispr_tearsheet_04_16_2020.txt \
    --aggLocation <some_path_here>/data/pipeline_output/ \
    --expName 2020_04_16_new_libs \
    --web <where_to_put_webfiles> \
    -s <path_to_github_clone_on_filesystem>/SingleCellLineage/scripts/ \
    -b <path_to_external_tools>/dartfs-hpc/rc/lab/M/McKennaLab/resources/tools/bin/ \
    -run

```

We generally run java with ~4g of memory; this is overkill but generally not a big issue on large compute environments. The ```-jar``` parameter is the path to the Queue execution manager, which runs the ```-S``` script for us. This script takes the ```-i``` tearsheet information, parses it out, and creates a number of compute jobs to transform the raw reads into GESTALT barcode calls. You also need to specify the aggregate data location with ```--aggLocation``` parameter; this is where the pipeline will put a couple files about the run as a whole (across samples). The ```--expName``` parameter gives a name to the experiment as a whole (used for web output and run statistics), and the ```--web``` directory is where the pipeline will dump web-visable plots for later analysis. The ```-s``` and ```-b``` parameters describe the location of tools we'll need to run the pipeline, and finally ```-run``` tells the script to actually start jobs (without ```-run``` it will just list the commands it would run).


## Run the example script

If you're running a locally installed version of the pipeline, you just need to run the script we created above:

```
sh <example_shell_script.sh>
```

Otherwise If you're running from within a docker container, run the example script:
```
sh /app/sc_GESTALT/tear_sheet_examples/run_crispr_pipeline.sh
```
## More Docker information

You can adjust the input files by changing filenames in the tearsheet: app/sc_GESTALT/tear_sheet_examples/basic_example.txt. To mount your data on a local disk to a location within the docker filesystem, use the ```-v``` option:
```
docker run -m 8g -it -p 8080:80 -v /Users/aaronmck/Desktop/gel_images/:/my_data aaronmck/single_cell_gestalt:SC_GESTALT /bin/bash
```
You can then rerun the example script or create your own:
```
sh /app/sc_GESTALT/tear_sheet_examples/run_crispr_pipeline.sh
```

# Output

A run of the GESTALT pipeline creates a number of files in the output directory of each sample. This location is specified in the input sample spreadsheet, in the *output.dir* column. See our example spreadsheet used in the script here: 

`/app/sc_GESTALT/tear_sheet_examples/basic_example.txt`

In that file the base output location is set to */app/data/*, which should have a sub-directory for the *dome_4_1X*. 

Within the example run output directory there are a couple important output files:

* **dome_4_1X.stats** contains the collapsed reads and the event calls per target site. This is the most important file, as it contains the final collapsed output for individual UMIs.

* **dome_4_1X.umiCounts** information about each UMIs that we saw and if there were enough reads for an individual UMI to call it 'successfully captured'. You should check this file to see if there are any problems with the UMI collapsing step.  

The pipeline also generates visualization output: plots for the editing pattern over the target. The default is to put this into the */var/www/html/* directory on the Docker container, but can be set with the --web parameter to the pipeline. If you're using docker and you've remapped your ports above (using -p 8080:80) you should be able to visualize the output by opening localhost:8080 when run locally. 

Within the the base output directory there will be a directory for each run. In the example case, our run is called *testdata*, and there will be a directory matching this name. In that, our sample is called *dome_4_1X*. This folder contains the html output to visualize editing patterns over your sample. If you've setup your port (the `-p 8080:80` option when running Docker) you can open a browser and view your output here:

http://localhost:8080/testdata/dome_4_1X/read_editing_mutlihistogram.html

Each sample processed should have a ```read_editing_mutlihistogram.html``` html file in it's base output directory.
