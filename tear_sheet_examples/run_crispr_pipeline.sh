/etc/init.d/lighttpd start

java -Xmx4g -jar /app/queue.jar \
 -S /app/sc_GESTALT/pipelines/CRISPR_analysis_PE_V2.scala  \
 -i /app/sc_GESTALT/tear_sheet_examples/no_barcode_splitting_example_tearsheet.txt \
 --aggLocation /var/www/html/testdata/ \
 --expName testdata \
 --eda /app/EDNAFULL.Ns_are_zero \
 -run \
 -resMemReqParam mfree \
 -s /app/sc_GESTALT/scripts/ \
 -b /app/bin/
 
