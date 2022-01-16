a [Quark](https://supercollider-quarks.github.io/quarks/) for [SuperCollider](https://supercollider.github.io)

install it from within supercollider with the command `Quarks.install("redSampler")`

# redSampler
Playback of soundfiles from disk or RAM. With simple envelope, voices and looping. The 'giga' sampler class (`RedDiskInSamplerGiga`) is useful for massive sample libraries that will not fit in RAM. It preloads a bit of the beginning of all the soudfiles and streams the rest from disk when needed.

Basic usage:

```supercollider
s.boot;
a= RedSampler();
a.prepareForPlay(\snd1, Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff");
a.play(\snd1);
a.free;

//then there are simple wrapper classes like
//RedLoop for looping a soundfile from disk
a= RedLoopDisk(Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff")
a.amp= 0.3;
a.free;
```
