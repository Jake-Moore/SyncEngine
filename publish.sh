# Check if GITHUB_REPOSITORY is present to change the root command
root_command="gradle"
if [ -z "$GITHUB_REPOSITORY" ]; then
  root_command="./gradlew"

  # Default value is true
  bClean=true
  # Check if an argument is provided
  if [ $# -gt 0 ]; then
      # Check if the argument is "false"
      if [ "$1" = "false" ]; then
          bClean=false
      fi
  fi

  if [ "$bClean" = true ]; then
    echo "$root_command clean"
    "$root_command" clean || exit 1
  fi
fi

echo "$root_command :core:publish"
"$root_command" :core:publish || exit 1

echo "$root_command :spigot-jar:publish"
"$root_command" :spigot-jar:publish || exit 1