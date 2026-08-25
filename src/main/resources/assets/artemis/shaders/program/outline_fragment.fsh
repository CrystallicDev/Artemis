#version 120

uniform sampler2D texture;
uniform vec2 resolution;

void main() {
    vec2 texelSize = 1.0 / resolution;
    vec2 uv = gl_TexCoord[0].xy;

    // Empêche d’échantillonner en dehors de la texture
    vec2 safeUV = clamp(uv, texelSize, 1.0 - texelSize);

    // ────────────────
    // Étape 1 : Échantillonnage voisin
    // ────────────────
    vec4 tl = texture2D(texture, safeUV + vec2(-texelSize.x, -texelSize.y));
    vec4 t  = texture2D(texture, safeUV + vec2(0.0, -texelSize.y));
    vec4 tr = texture2D(texture, safeUV + vec2(texelSize.x, -texelSize.y));
    vec4 l  = texture2D(texture, safeUV + vec2(-texelSize.x, 0.0));
    vec4 c  = texture2D(texture, safeUV);
    vec4 r  = texture2D(texture, safeUV + vec2(texelSize.x, 0.0));
    vec4 bl = texture2D(texture, safeUV + vec2(-texelSize.x, texelSize.y));
    vec4 b  = texture2D(texture, safeUV + vec2(0.0, texelSize.y));
    vec4 br = texture2D(texture, safeUV + vec2(texelSize.x, texelSize.y));

    // ────────────────
    // Étape 2 : Sobel sur la luminance
    // ────────────────
    vec3 weights = vec3(0.299, 0.587, 0.114);

    float lumTL = dot(tl.rgb, weights);
    float lumT  = dot(t.rgb,  weights);
    float lumTR = dot(tr.rgb, weights);
    float lumL  = dot(l.rgb,  weights);
    float lumC  = dot(c.rgb,  weights);
    float lumR  = dot(r.rgb,  weights);
    float lumBL = dot(bl.rgb, weights);
    float lumB  = dot(b.rgb,  weights);
    float lumBR = dot(br.rgb, weights);

    float gx = -lumTL + lumTR - 2.0*lumL + 2.0*lumR - lumBL + lumBR;
    float gy = -lumTL - 2.0*lumT - lumTR + lumBL + 2.0*lumB + lumBR;
    float edge = sqrt(gx*gx + gy*gy);

    // Contours plus fins et doux
    float alpha = smoothstep(0.15, 0.4, edge);
    alpha = pow(alpha, 0.85);

    vec4 baseColor = vec4(c.rgb, alpha);

    // ────────────────
    // Étape 3 : FXAA ultra-léger
    // ────────────────
    float lumaMin = min(lumC, min(min(min(lumTL, lumT), min(lumTR, lumL)), min(min(lumR, lumBL), min(lumB, lumBR))));
    float lumaMax = max(lumC, max(max(max(lumTL, lumT), max(lumTR, lumL)), max(max(lumR, lumBL), max(lumB, lumBR))));
    float lumaRange = lumaMax - lumaMin;

    // Si peu de contraste, pas besoin de FXAA
    if (lumaRange < 0.1) {
        gl_FragColor = baseColor;
        return;
    }

    // Direction du blend
    vec2 dir;
    dir.x = -((lumTL + lumL + lumBL) - (lumTR + lumR + lumBR));
    dir.y =  ((lumTL + lumT + lumTR) - (lumBL + lumB + lumBR));

    float dirReduce = max((lumTL + lumT + lumTR + lumL + lumC + lumR + lumBL + lumB + lumBR) / 9.0 * 0.25, 0.001);
    float rcpDirMin = 1.0 / (min(abs(dir.x), abs(dir.y)) + dirReduce);
    dir = clamp(dir * rcpDirMin * texelSize * 0.5, vec2(-8.0), vec2(8.0));

    vec4 result1 = texture2D(texture, safeUV + dir * (1.0 / 3.0 - 0.5));
    vec4 result2 = texture2D(texture, safeUV + dir * (2.0 / 3.0 - 0.5));
    vec4 fxaaColor = (result1 + result2) * 0.5;

    // ────────────────
    // Étape 4 : Mélange final
    // ────────────────
    // On pondère le mix selon l’alpha du contour pour éviter les noirs durs
    gl_FragColor = vec4(mix(fxaaColor.rgb, baseColor.rgb, baseColor.a * 0.6), baseColor.a);
}

