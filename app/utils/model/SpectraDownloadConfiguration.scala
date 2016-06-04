package utils.model

/**
  * Created by radiokoza on 4.6.16.
  */
case class SpectraDownloadConfiguration(directory: Directory,
                                        authorization: Option[Authorization],
                                        datalink: Option[DatalinkConfig])
