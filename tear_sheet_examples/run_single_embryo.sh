java -Xmx4g -jar /net/gs/vol1/home/aaronmck/tools/queue-protected/protected/gatk-queue-package-distribution/target/gatk-queue-package-distribution-3.5.jar \
 -S /net/shendure/vol10/projects/CRISPR.lineage/nobackup/codebase/pipelines/CRISPR_analysis_PE_V2.scala  \
 -i /net/shendure/vol10/projects/CRISPR.lineage/nobackup/2016_04_07_Embryos_rerun/data/crispr_tearsheet.txt  \
 --aggLocation /net/shendure/vol10/projects/CRISPR.lineage/nobackup/2016_04_07_Embryos_rerun/data/pipeline_output/aggregate/ \
 --expName 2016_04_07_Embryos_rerun \
 -run -qsub -resMemReqParam mfree \
 --minimumUMIReads 10 \
 --minimumSurvivingUMIReads 6
