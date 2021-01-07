java -Xmx4g -jar /app/queue.jar \
 -S /app/sc_GESTALT/pipelines/CRISPR_analysis_PE_V2.scala  \
 -i /app/sc_GESTALT/tear_sheet_examples/basic_example.txt \
 --aggLocation /var/www/html/testdata/ \
 --expName testdata \
 --eda /app/EDNAFULL.Ns_are_zero \
 -run \
 -resMemReqParam mfree \
 -s /app/sc_GESTALT/scripts/ \
 -b /app/bin/ \
 --scala "/usr/bin/scala -nocompdaemon" 
 
