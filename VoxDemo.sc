/*
TODO:
1. Origanize the gui script with classes
2. SynthDef chain: read the previous one in the bus, add filter, and out
3. MIDI control: Korg MicroKontrol, map knobs

Issues:
1. "SynthDef not found" when run Vox_main.new for the first time. But it works if we close the window and run it again.
*/

Vox_main{
	var modules, src, filter;

	// this is a normal constructor method
	*new { // * indicates this is a class method
		/* | arga, argb, argc | */
        ^super.new.init(/*arga, argb, argc*/)
    }

    init {
		/* | arga, argb, argc | */
		modules = [
			["Audio Source", "Formants"],
			[Vox_source, Vox_filter]
		];
		Server.default.waitForBoot({
			postln("Server booted.");
			this.createPanel;
			this.compileSynthDefs;
			Server.default.sync;
		});
    }

	compileSynthDefs{
		modules[1].do {|class| if (class.notNil, { class.compileSynthDefs } )}
	}

	createPanel{
		var window, srcView, filterView;

		window = Window("Vox Demo", Rect(
			Window.screenBounds.width/2-100,
			Window.screenBounds.height/2-100,
			600,
			400), scroll: true)
		.front
		.alwaysOnTop_(true);
		window.onClose({
			// TODO: free the synth or stop sounds if window is closed
		});

		src = Vox_source.new;
		srcView = src.view;

		filter = Vox_filter.new;
		filterView = filter.view;

		window.layout = VLayout(HLayout(srcView, nil), filterView, nil);
		window.layout.margins_(1);
	}

}

Vox_source{
	/*
	TODO:
	1. Improve volume control scaling to log or exp. (X)
	  1.1 Use Server.volume? Changes the internal volume setting. May not be good
	2.
	*/
	var <view;

	*new { arg main;
		^super.new.init(main)
	}

	init { arg main;
		//this.compileSynthDefs;
		this.createPanel;
	}

	*compileSynthDefs {
		SynthDef(\Voxsource, {
			arg outBus=0,
			note=45, mul=2.0, fv=6.5;
			var source;

			source = Blip.ar(SinOsc.kr(fv, 0, 0.01, note.midicps), 200, mul:mul);
			Out.ar(outBus, source);
			};
		).send(Server.default);
	}

	createPanel {
		var synth, title, button, noteBox, noteLabel, volSlider, volLabel, text;

		synth = Synth.new(\Voxsource);
		view = View()
		.background_(Color.gray.alpha_(0.3))
		.minSize_(Size(150, 80));
		// .resize_(5);

		title = StaticText(view, 80@30).string_("Source sound");
		text = TextField(view, 80@60).string_("Here: vibrato and stuff.");

		button = Button(view,40@30)
		.states_([
			["Mute", Color.black, Color.gray(0.8)],  // value=0
			["Unmute", Color.white, Color(0.4, 1.0, 0.6)]    // value=1
		]);
		button.action_({
			arg obj;
			if(
				obj.value == 1,
				{synth.set(\mul, 0)},
				{synth.set(\mul, 1)}
			);
		});

		noteLabel = StaticText(view, 60@30).string_("MIDI Note");
		noteBox = NumberBox(view, 60@30)   // used to be an EZNumber which is more handy to program,
		.clipLo_(21).clipHi_(108).step_(1);// but crashes sometimes
		noteBox.action_({
			arg obj;
			var f;
			f = obj.value;
			synth.set(\note, f);
		})
		.valueAction_(45);

		volLabel = StaticText(view, 20@20).string_("Vol");
		volSlider = Slider(view, 20@60).value_(0.1);
		volSlider.action_({
			arg obj;
			var m;
			m = (obj.value.linexp(0,1, 0.1, 10));
			synth.set(\mul, m);
			if(obj.value > 0, {button.value_(Mute);});
		});

		// Layout
		view.layout = HLayout(
			VLayout(
				HLayout(title, button),
				HLayout(noteLabel, noteBox, nil),
				text,
				nil
			),
			VLayout(volLabel, volSlider, nil),
			nil
		);
	}

}

Vox_filter{
	var <view, modules, visibleFormants;

	*new { arg main;
		^super.new.init(main)
	}

	init { arg main;
		modules = [
			["Formant"],
			[Vox_formant]
		];
		this.createPanel;
		// this.compileSynthDefs;
	}

	*compileSynthDefs{
		//modules[1].do {|class| if (class.notNil, { class.compileSynthDefs } )}
	}

	createPanel{
		var reLayout,
		f1, f1View;

		/*reLayout{
			// renew the synth once a new formant is enabled
			visibleFormants = [];

		};*/

		view = View()
		.minSize_(Size(500, 200))
		.background_(Color.gray.alpha_(0.4));

		f1 = Vox_formant.new;
		f1View = f1.view;

		view.layout = HLayout(f1View, nil);

	}

}

Vox_formant{
    var <view, audioOuts, audioIns, <inputBus;

	*new { arg main;
		^super.new.init(main)
	}

	init { arg main;
		audioOuts = Server.local.options.numOutputBusChannels;
		audioIns  = Server.local.options.numInputBusChannels;
		inputBus = audioOuts;  /* index of first input, by default */
		this.createPanel;
		// this.compileSynthDefs;
	}

	*compileSynthDefs{
		SynthDef(\Voxformant, {
			arg inBus=0, outBus=0,
			freq=150, res=0.8, low=0.1, band=0.0, high=0.0, notch=0.0, peak=0.0;
			var signal;

			signal = In.ar(inBus, 2);  // intend to get the signal from inBus
			signal = SVF.ar(signal,    // add a filter
				freq, //F1 (150,1000)
				res,
				low, band, high, notch, peak, mul: 1);
			Out.ar(outBus, signal);
		};
		).send(Server.default);
	}

	createPanel{
		var synth, fLabel, fUnit, freqBox;

		view = View()
		.minSize_(Size(60, 60))
		.background_(Color(0.5, 0.5, 1));

		fLabel = StaticText(view, 60@30).string_("Formant 1");
		fUnit = StaticText(view, 20@30).string_("Hz");
		freqBox = NumberBox(view, 40@30)
		.clipLo_(150).clipHi_(1000)
		.step_(0.1)
		.decimals_(1);

		/*synth = Synth(\Voxformant);

		freq.action_({
			arg obj;
			var f;
			f = obj.value.linexp(0,1,150,1000);
			synth.set(\freq, f);
		});*/

		/* // EZ Number Box: does not show up sometimes
		freqBox = EZNumber(ezView, 100@30,
			"Formant", [150, 1000, \exp, 0.1, 150, "Hz"].asSpec,
			initVal: 150, labelWidth: 70, numberWidth: 30);
		freqBox.setColors(Color.gray(0.2),Color.white);
		freqBox.action_({|obj| freqSlider.valueAction_(obj.value);});*/

		view.layout = VLayout(fLabel, HLayout(freqBox, fUnit, nil), nil);
	}
}