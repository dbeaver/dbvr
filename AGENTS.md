# dbvr community

- This repository keeps source code for dbvr - CLI interface of DBeaver
- Doesn't have any desktop UI and web interface modules
- Follows the same rules as for `dbeaver` repo

## Code review

- Report defects introduced by the PR. Cite the file and line, explain when the defect occurs, and suggest a focused fix.
- Flag unintended changes to public APIs, persisted data, configuration, or existing user workflows.
- For a bug fix, check that a regression test covers the failing case. Flag tests weakened to make the change pass.

### Security

- Review changed code and its direct callers and callees. For each security finding, identify who can trigger it and what they can access or change.
- Trace untrusted input into SQL, process execution, file paths, URLs, XML parsing, and deserialization. Check validation, escaping, and resource limits at the point of use.
- Check authentication, authorization, and default access. For operations accessible from Team Edition, verify that the server enforces the appropriate REALM permission. Verify that requests without the required permissions are rejected.
- Flag exposed credentials, connection secrets, or personal data in source, tests, configuration, logs, errors, and documentation. Do not quote sensitive values in the review.
- Check temporary-file permissions, archive extraction paths, redirect destinations, and TLS certificate verification when those operations change.

### OSGi and product features

- Keep OSGi model bundles usable headlessly. Flag model code or bundle manifests that depend on UI bundles, including SWT and JFace.
- Verify that each new bundle reaches its intended dbvr product through an included feature. Pro includes the shared `org.dbvr.cli.feature`; check shared bundles there once, without looking for a duplicate Pro entry.
