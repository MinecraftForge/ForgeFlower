# ForgeFlower
Forge's modifications to FernFlower. Fixing various bugs/inconsistencies. Main Repo: https://github.com/MinecraftForge/FernFlower

# Development setup
 - Make sure to use Java 9 or higher for gradle, Java 8 will not work for this repository!
 - Clone this repository, including submodules (`git clone --recurse-submodules git@github.com:MinecraftForge/ForgeFlower` or `git clone --recurse-submodules https://github.com/MinecraftForge/ForgeFlower`)
 - Run the `applyPatches` gradle task
 - Work in the "ForgeFlower" directory. It contains the FernFlower repository with all Forge changes applied. Re-running `applyPatches` will override your changes, as well as a lot of other gradle tasks that depend on `applyPatches`
 - Commit the results in the ForgeFlower directory (not the main git repository)
 - Run the `makePatches` gradle task. This should generate a new patch in "FernFlower-Patches".
 - Commit the new patch in the main repository