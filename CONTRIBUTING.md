# Pull Requests

 - Include a diff of the code of the current Minecraft version with and without the changes. An easy way to create this diff:
   - Use [MCPConfig](https://github.com/MinecraftForge/MCPConfig) to decompile MC
   - Add the resulting code to a local git repository:
     - Open a terminal in the directory with the source code
     - run `git init`, `git add src` and `git commit -m "Initial commit with standard ForgeFlower"`
   - Change the FF version in  MCPConfig in `versions/<your MC version>/config.json` and add a build of your branch in `build/libraries/net/minecraftforge/forgeflower`
   - Decompile again
   - Commit the result (`git add src`, `git commit -m "Changes due to the FF changes"`) and pipe the output of `git diff HEAD~1 HEAD` into a file. This is the required diff.
 - Explain the purpose of the PR and, if it isn't very obvious from the code, how it is achieved
 - Include a test case, a class that decompiles incorrectly without the PR and correctly with the PR
 - Don't reformat code. The patches should be small, but still use the same code style as the rest of the project (as opposed to patches for [Forge](https://github.com/MinecraftForge/MinecraftForge))
