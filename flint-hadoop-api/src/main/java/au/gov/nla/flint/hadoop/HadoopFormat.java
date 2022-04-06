package au.gov.nla.flint.hadoop;

/**
 * Interface to be implemented by a {@link au.gov.nla.flint.formats.Format} implementation
 * in case additional hadoop functionality wants to be implemented via
 * {@link au.gov.nla.flint.hadoop.AdditionalMapTasks}
 */
public interface HadoopFormat {
    /**
     * Get tasks in addition to the standard behaviour of a FlintHadoop mapper.
     * @return {@link au.gov.nla.flint.hadoop.AdditionalMapTasks}
     */
    public AdditionalMapTasks getAdditionalMapTasks();
}
