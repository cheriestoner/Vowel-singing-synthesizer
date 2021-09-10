/*
TODO:
1. Origanize the gui script with classes
2. SynthDef chain: read the previous one in the bus, add filter, and out
3. MIDI control: Korg MicroKontrol, map knobs
*/

Vox_main{
	var modules, src, filter;

	// this is a normal constructor method
	*new { /* | arga, argb, argc | */    // * indicates this is a class method
        ^super.new.init(/*arga, argb, argc*/)
    }

    init { // | arga, argb, argc |
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

		src = Vox_source.new;
		srcView = src.view;

		filter = Vox_filter.new;
		filterView = filter.view;

		window.layout = VLayout(HLayout(srcView, nil), filterView, nil);
		window.layout.margins_(1);
	}

}

Vox_source{
	var <view;

	*new { arg main;
		^super.new.init(main)
	}

	init { arg main;
		//this.compileSynthDefs;
		this.createPanel;
	}

	*compileSynthDefs { // Or *compileSynthDefs
		SynthDef(\Voxsource, {
			arg outBus=0,
			note=45, volume=2.0, fv=6.5;
			var source;

			source = Blip.ar(SinOsc.kr(fv, 0, 0.01, note.midicps), 200, mul:volume);
			Out.ar(outBus, source);
			};
		).send(Server.default);
	}

	createPanel {
		var synth, title, button, noteBox, noteLabel, volSlider, ezView, text;

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
		])
		.action_({
			arg obj;
			if(
				obj.value == 1,
				{synth.set(\volume, 0)},
				{synth.set(\volume, 1)}
			);
		});

		noteLabel = StaticText(view, 60@30).string_("MIDI Note");
		noteBox = NumberBox(view, 60@30)
		.clipLo_(21)
		.clipHi_(108)
		.step_(1)
		.action_({
			arg obj;
			var f;
			f = obj.value;
			synth.set(\note, f);
		})
		.valueAction_(45);


		/*ezView = View(view, 120@30); // contains an EZNumber,
		//an EZ... object itself can not be included in a layout
		noteBox = EZNumber(ezView, 120@30,
			"MIDI Note", [21, 108, \lin, 1.0, 45].asSpec,
			{|ez| synth.set(\note, ez.value)},
			45, true, 80, 40);
		noteBox.setColors(Color.rand,Color.white);*/

		volSlider = Slider(view, 20@80)
		.value_(0.1)
		.action_({
			arg obj;
			var vol;
			vol = (obj.value * 10.0);
			synth.set(\volume, vol);
		});

		view.layout = HLayout(
			[VLayout(
				[HLayout(title, button)],
				//[ezView],
				HLayout(noteLabel, noteBox, nil),
				text,
				nil
			)],
			[volSlider],
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

		/*f1 = Vox_formant.new;
		f1View = f1.view;

		view.layout = HLayout(f1View, nil);*/

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

		synth = Synth(\Voxformant);

		fLabel = StaticText(view, 60@30).string_("Formant 1");
		fUnit = StaticText(view, 20@30).string_("Hz");
		freqBox = NumberBox(view, 40@30)
		.clipLo_(150)
		.clipHi_(1000)
		.step_(0.1)
		.decimals_(1)
		.action_({
			arg obj;
			var f;
			f = obj.value.linexp(0,1,150,1000);
			synth.set(\freq, f);
		});
		/* // EZ Number Box: does not show up sometimes
		freqBox = EZNumber(ezView, 100@30,
			"Formant", [150, 1000, \exp, 0.1, 150, "Hz"].asSpec,
			initVal: 150, labelWidth: 70, numberWidth: 30);
		freqBox.setColors(Color.gray(0.2),Color.white);
		freqBox.action_({|obj| freqSlider.valueAction_(obj.value);});*/

		view.layout = VLayout(fLabel, HLayout(freqBox, fUnit, nil), nil);
	}
}