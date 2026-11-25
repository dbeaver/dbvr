# dbvr community

- This repository keeps source code for dbvr - CLI interface of DBeaver
- Doesn't have any desktop UI and web interface modules
- Follows the same rules as for `dbeaver` repo

## Code review

- Report defects introduced by the PR. Cite the file and line, explain when the defect occurs, and suggest a focused fix.
- Flag unintended changes to public APIs, persisted data, configuration, or existing user workflows.
- For a bug fix, check that a regression test covers the failing case. Flag tests weakened to make the change pass.

### Security

- Review changed code and its direct call paths. Report a security finding only when the PR introduces a realistic attack path. Cite the file and line, identify who can trigger it and the impact, and suggest a focused fix.
- Trace input from users, database contents, configuration, and remote services into SQL, shell commands, file paths, URLs, XML parsing, and deserialization. Check validation, encoding, escaping, canonicalization, and resource limits at the point of use.
- Check authentication, authorization, and default access. For operations accessible from Team Edition, verify that the server enforces the appropriate REALM permission. Verify that requests without the required permissions are rejected.
- Flag unsafe handling of credentials and database connection settings. Check code, tests, configuration, logs, exceptions, and documentation for exposed secrets, tokens, passwords, private keys, connection strings, or personal data. Do not quote sensitive values in the review.
- Check temporary-file permissions, archive extraction paths, redirects, TLS configuration (including certificate verification), and external-process execution when those operations change.

### OSGi and product features

- Keep OSGi model bundles usable headlessly. Flag model code or bundle manifests that depend on UI bundles, including SWT and JFace.
- Verify that each new bundle reaches its intended dbvr product through an included feature. Pro includes the shared `org.dbvr.cli.feature`; check shared bundles there once, without looking for a duplicate Pro entry.

### Interface text

Review changed human-readable CLI messages and confirmations. Exclude technical documentation and release notes unless the text is presented in the interface.

In messages, descriptions, and other explanatory prose, use normal sentence punctuation: end complete sentences with a period, including standalone messages, and do not add a period to a fragment.

Do not use contractions in interface text. Write words in full, such as `The connection cannot be established` rather than `The connection can't be established`. Contractions are acceptable in longer reading-oriented content, such as release notes.

Use a question mark only in a confirmation that asks a direct question. Write informational messages as statements. Do not use exclamation points in interface text.

Report only clear violations introduced by the pull request and suggest the correct user-visible wording.
