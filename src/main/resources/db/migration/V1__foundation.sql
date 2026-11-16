CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE documents (
    id BINARY(16) PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE document_keys (
    document_id BINARY(16) PRIMARY KEY,
    wrapped_key VARBINARY(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_document_keys_document FOREIGN KEY (document_id) REFERENCES documents(id)
);

CREATE TABLE document_versions (
    id BINARY(16) PRIMARY KEY,
    document_id BINARY(16) NOT NULL,
    version_number INT NOT NULL,
    encrypted_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    edited_by BIGINT NULL,
    change_note VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_document_version UNIQUE (document_id, version_number),
    CONSTRAINT fk_versions_document FOREIGN KEY (document_id) REFERENCES documents(id),
    CONSTRAINT fk_versions_editor FOREIGN KEY (edited_by) REFERENCES users(id)
);

CREATE TABLE access_policies (
    id BINARY(16) PRIMARY KEY,
    document_id BINARY(16) NOT NULL,
    token VARCHAR(100) UNIQUE NULL,
    max_print_count INT NULL,
    current_print_count INT NOT NULL DEFAULT 0,
    max_edit_count INT NULL,
    current_edit_count INT NOT NULL DEFAULT 0,
    expiry_timestamp TIMESTAMP(6) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_policies_document FOREIGN KEY (document_id) REFERENCES documents(id),
    CONSTRAINT fk_policies_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE access_policy_actions (
    policy_id BINARY(16) NOT NULL,
    action VARCHAR(20) NOT NULL,
    PRIMARY KEY (policy_id, action),
    CONSTRAINT fk_policy_actions_policy FOREIGN KEY (policy_id) REFERENCES access_policies(id)
);

CREATE TABLE action_requests (
    id BINARY(16) PRIMARY KEY,
    document_id BINARY(16) NOT NULL,
    policy_id BINARY(16) NOT NULL,
    type VARCHAR(20) NOT NULL,
    requested_by BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    pending_version_id BINARY(16) NULL,
    transaction_id BINARY(16) NULL,
    decision_note VARCHAR(1000) NULL,
    requested_at TIMESTAMP(6) NOT NULL,
    decided_at TIMESTAMP(6) NULL,
    released_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_requests_document FOREIGN KEY (document_id) REFERENCES documents(id),
    CONSTRAINT fk_requests_policy FOREIGN KEY (policy_id) REFERENCES access_policies(id),
    CONSTRAINT fk_requests_user FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT fk_requests_version FOREIGN KEY (pending_version_id) REFERENCES document_versions(id)
);

CREATE TABLE audit_logs (
    id BINARY(16) PRIMARY KEY,
    actor_id BIGINT NULL,
    document_id BINARY(16) NULL,
    action VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    timestamp TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT fk_audit_document FOREIGN KEY (document_id) REFERENCES documents(id)
);

CREATE INDEX idx_access_policy_token ON access_policies(token);
CREATE INDEX idx_access_policy_expiry ON access_policies(expiry_timestamp);
CREATE INDEX idx_action_request_status ON action_requests(status);
CREATE INDEX idx_audit_document_timestamp ON audit_logs(document_id, timestamp);
