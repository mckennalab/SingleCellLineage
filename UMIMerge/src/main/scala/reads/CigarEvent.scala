package reads

/**
 * the cigar event -- encoding our basic cigar characters, which are different than the SAM specification
  *
  * @param encoding the encodings string
 */
abstract class CigarEvent(var encoding: String = "U")
