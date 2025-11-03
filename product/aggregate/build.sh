#!/usr/bin/env sh

script_dir="$(realpath "$(dirname "$0")")"
"$script_dir/../../../dbeaver-common/mvnw" clean package -T1C -Dheadless-platform -Pall-platforms -f "$script_dir/pom.xml"
