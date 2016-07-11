java -Xmx4g -jar /net/gs/vol1/home/aaronmck/tools/queue-protected/protected/gatk-queue-package-distribution/target/gatk-queue-package-distribution-3.5.jar \
 -S /net/shendure/vol10/projects/CRISPR.lineage/nobackup/codebase/pipelines/CRISPR_analysis_PE_V2.scala  \
 -i /net/shendure/vol10/projects/CRISPR.lineage/nobackup/2016_06_21_Adult_Fish_7_9_12/data/crispr_tearsheet.txt  \
 --aggLocation /net/shendure/vol10/projects/CRISPR.lineage/nobackup/2016_06_21_Adult_Fish_7_9_12/data/pipeline_output/aggregate/ \
 --expName 2016_06_21_Adult_Fish_7_9_12 \
 --eda /net/shendure/vol10/projects/CRISPR.lineage/nobackup/reference_data/EDNAFULL.for_molly \
 -run -qsub -resMemReqParam mfree
