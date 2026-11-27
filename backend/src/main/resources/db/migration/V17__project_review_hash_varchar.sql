-- Hibernate maps @Column(length=64) String fields to VARCHAR, not fixed-width CHAR.
-- Preserve existing hashes while aligning the PostgreSQL column with the entity mapping.
ALTER TABLE project_reviews
    ALTER COLUMN artifact_sha256 TYPE VARCHAR(64)
    USING rtrim(artifact_sha256)::VARCHAR(64);
