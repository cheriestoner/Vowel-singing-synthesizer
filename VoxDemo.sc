/*
TODO:
1. Origanize the gui script with classes
2. SynthDef chain: read the previous one,add filter
3. MIDI control: Korg MicroKontrol, map knobs
*/

Vox_main{

	*new{
		^super.new.init()
	}
}

/*VoxControl{
	var modules;

	// this is a normal constructor method
    *new { /* | arga, argb, argc | */
        ^super.new.init(/*arga, argb, argc*/)
    }

    init { // | arga, argb, argc |
		modules = [

		];
		Server.default.waitForBoot({
			postln("Server booted.");
			this.createPanel;
			this.compileSynthDefs;
			Server.default.sync;
		});
    }

	compileSynthDefs{}

	createPanel{
		var window;

		w = Window("Vox Demo", Rect(
			Window.screenBounds.width/2-100,
			Window.screenBounds.height/2-100,
			400,
			400), scroll: true)
		.front
		.alwaysOnTop_(true);

	}



}*/