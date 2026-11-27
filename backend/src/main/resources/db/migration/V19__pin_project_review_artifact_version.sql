ALTER TABLE project_reviews ADD COLUMN artifact_version_id BIGINT;
ALTER TABLE project_reviews ADD COLUMN artifact_version_number INTEGER;
ALTER TABLE project_reviews ADD CONSTRAINT fk_project_review_artifact_version
    FOREIGN KEY (artifact_version_id) REFERENCES project_artifact_versions(id) ON DELETE SET NULL;
CREATE INDEX idx_project_reviews_artifact_version ON project_reviews(artifact_version_id);
