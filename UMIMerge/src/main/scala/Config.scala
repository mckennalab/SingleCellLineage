import java.io.File

/**
  * The basic configuration class to store all our information in
  */
case class Config(inputFileReads1: Option[File] = None,
                  inputFileReads2: Option[File] = None,
                  outputFastq1: Option[File] = None,
                  outputFastq2: Option[File] = None,
                  outputUMIStats: Option[File] = None,
                  umiLength: Int = 10,
                  umiStartPos: Int = 0,
                  primersEachEnd: Option[File] = None,
                  samplename: String = "TEST",
                  minimumUMIReads: Int = 10,
                  minimumSurvivingUMIReads: Int = 6,
                  umiInForwardRead: Boolean = true,
                  downsampleSize: Int = 40,
                  primersToCheck: String = "BOTH",
                  primerMismatches: Int = 7,
                  processSingleReads: Boolean = false)
